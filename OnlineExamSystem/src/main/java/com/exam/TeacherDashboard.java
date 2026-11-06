package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TeacherDashboard extends JFrame {

    private JTable questionTable;
    private DefaultTableModel model;
    private int teacherId;
    private String teacherName;

    public TeacherDashboard(int teacherId, String teacherName) {

        this.teacherId = teacherId;
        this.teacherName = teacherName;

        setTitle("Teacher Dashboard");
        setSize(1000, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel(
                "TEACHER DASHBOARD - " + teacherName,
                SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JButton addQuestion = new JButton("Add Question");
        JButton refresh = new JButton("Refresh");
        JButton createExam = new JButton("Create Exam");
        JButton viewResults = new JButton("View Results");
        JButton analytics = new JButton("Analytics");
        JButton logout = new JButton("Logout");

        model = new DefaultTableModel(
                new String[]{
                        "ID", "Subject", "Question",
                        "Type", "Difficulty", "Marks"
                }, 0
        );

        questionTable = new JTable(model);

        JPanel buttons = new JPanel();

        buttons.add(addQuestion);
        buttons.add(refresh);
        buttons.add(createExam);
        buttons.add(viewResults);
        buttons.add(analytics);
        buttons.add(logout);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(questionTable), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addQuestion.addActionListener(e -> addQuestion());

        refresh.addActionListener(e -> loadQuestions());

        createExam.addActionListener(e ->
                new CreateExamFrame(teacherId, teacherName));

        viewResults.addActionListener(e ->
                new TeacherResultsFrame(teacherId, teacherName));

        analytics.addActionListener(e ->
                new TeacherAnalyticsFrame(teacherId, teacherName));

        logout.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        loadQuestions();

        setVisible(true);
    }

    private void loadQuestions() {

        model.setRowCount(0);

        String sql = """
            SELECT q.question_id, s.subject_name,
                   q.question_text, q.question_type,
                   q.difficulty, q.marks
            FROM questions q
            JOIN subjects s ON q.subject_id = s.subject_id
            WHERE q.created_by = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, teacherId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("question_id"),
                        rs.getString("subject_name"),
                        rs.getString("question_text"),
                        rs.getString("question_type"),
                        rs.getString("difficulty"),
                        rs.getDouble("marks")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void addQuestion() {

        JComboBox<String> subjects = new JComboBox<>();

        JComboBox<String> types = new JComboBox<>(
                new String[]{"MCQ", "TRUE_FALSE"});

        JComboBox<String> difficulty = new JComboBox<>(
                new String[]{"EASY", "MEDIUM", "HARD"});

        JTextField question = new JTextField();
        JTextField marks = new JTextField("1");

        JTextField option1 = new JTextField();
        JTextField option2 = new JTextField();
        JTextField option3 = new JTextField();
        JTextField option4 = new JTextField();

        JComboBox<String> correct = new JComboBox<>(
                new String[]{"Option 1", "Option 2",
                        "Option 3", "Option 4"});

        loadSubjects(subjects);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        panel.add(new JLabel("Subject"));
        panel.add(subjects);

        panel.add(new JLabel("Question"));
        panel.add(question);

        panel.add(new JLabel("Type"));
        panel.add(types);

        panel.add(new JLabel("Difficulty"));
        panel.add(difficulty);

        panel.add(new JLabel("Marks"));
        panel.add(marks);

        panel.add(new JLabel("Option 1"));
        panel.add(option1);

        panel.add(new JLabel("Option 2"));
        panel.add(option2);

        panel.add(new JLabel("Option 3"));
        panel.add(option3);

        panel.add(new JLabel("Option 4"));
        panel.add(option4);

        panel.add(new JLabel("Correct Answer"));
        panel.add(correct);

        types.addActionListener(e -> {

            boolean isMCQ = types.getSelectedItem().equals("MCQ");

            option3.setEnabled(isMCQ);
            option4.setEnabled(isMCQ);
            correct.setEnabled(isMCQ);
        });

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Add Question",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
            return;

        if (subjects.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Please create a subject first.");
            return;
        }

        try {

            int subjectId = Integer.parseInt(
                    subjects.getSelectedItem().toString()
                            .split(" - ")[0]);

            double questionMarks = Double.parseDouble(marks.getText());

            String questionType = types.getSelectedItem().toString();

            if (question.getText().trim().isEmpty()
                    || questionMarks <= 0
                    || option1.getText().trim().isEmpty()
                    || option2.getText().trim().isEmpty()) {

                JOptionPane.showMessageDialog(this,
                        "Fill all required fields correctly.");
                return;
            }

            if (questionType.equals("MCQ")
                    && (option3.getText().trim().isEmpty()
                    || option4.getText().trim().isEmpty())) {

                JOptionPane.showMessageDialog(this,
                        "Enter all four MCQ options.");
                return;
            }

            if (questionType.equals("TRUE_FALSE")) {

                option1.setText("True");
                option2.setText("False");
                correct.setSelectedIndex(0);
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                try {

                    String sql = """
                        INSERT INTO questions
                        (subject_id, question_text, question_type,
                         difficulty, marks, created_by)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;

                    int questionId;

                    try (PreparedStatement ps =
                                 con.prepareStatement(
                                         sql,
                                         Statement.RETURN_GENERATED_KEYS)) {

                        ps.setInt(1, subjectId);
                        ps.setString(2, question.getText());
                        ps.setString(3, questionType);
                        ps.setString(4,
                                difficulty.getSelectedItem().toString());
                        ps.setDouble(5, questionMarks);
                        ps.setInt(6, teacherId);

                        ps.executeUpdate();

                        ResultSet keys = ps.getGeneratedKeys();
                        keys.next();

                        questionId = keys.getInt(1);
                    }

                    String[] optionTexts = {
                            option1.getText(),
                            option2.getText(),
                            option3.getText(),
                            option4.getText()
                    };

                    int correctIndex = correct.getSelectedIndex();

                    String optionSQL = """
                        INSERT INTO options
                        (question_id, option_text, is_correct)
                        VALUES (?, ?, ?)
                        """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(optionSQL)) {

                        int count = questionType.equals("MCQ") ? 4 : 2;

                        for (int i = 0; i < count; i++) {

                            ps.setInt(1, questionId);
                            ps.setString(2, optionTexts[i]);
                            ps.setBoolean(3, i == correctIndex);

                            ps.executeUpdate();
                        }
                    }

                    con.commit();

                    JOptionPane.showMessageDialog(this,
                            "Question added successfully!");

                } catch (SQLException ex) {

                    con.rollback();
                    throw ex;

                } finally {
                    con.setAutoCommit(true);
                }
            }

            loadQuestions();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage());
        }
    }

    private void loadSubjects(JComboBox<String> subjects) {

        String sql = "SELECT subject_id, subject_name FROM subjects";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                subjects.addItem(
                        rs.getInt("subject_id") + " - "
                                + rs.getString("subject_name"));
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
}