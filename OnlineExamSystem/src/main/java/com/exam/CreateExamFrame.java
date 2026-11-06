
package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreateExamFrame extends JFrame {

    private final int teacherId;

    private JComboBox<String> subjectCombo;
    private JTextField titleField;
    private JTextField durationField;
    private JTextField passMarksField;
    private JTextField startField;
    private JTextField endField;

    private JTable questionTable;
    private DefaultTableModel tableModel;

    private List<Integer> subjectIds = new ArrayList<>();

    public CreateExamFrame(int teacherId, String teacherName) {
        this.teacherId = teacherId;

        setTitle("Create Exam - " + teacherName);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        subjectCombo = new JComboBox<>();
        titleField = new JTextField();
        durationField = new JTextField();
        passMarksField = new JTextField();

        startField = new JTextField("2026-12-01 09:00:00");
        endField = new JTextField("2026-12-01 10:00:00");

        formPanel.add(new JLabel("Subject:"));
        formPanel.add(subjectCombo);

        formPanel.add(new JLabel("Exam Title:"));
        formPanel.add(titleField);

        formPanel.add(new JLabel("Duration (minutes):"));
        formPanel.add(durationField);

        formPanel.add(new JLabel("Pass Marks:"));
        formPanel.add(passMarksField);

        formPanel.add(new JLabel("Start Time (yyyy-MM-dd HH:mm:ss):"));
        formPanel.add(startField);

        formPanel.add(new JLabel("End Time (yyyy-MM-dd HH:mm:ss):"));
        formPanel.add(endField);

        JButton loadQuestionsButton = new JButton("Load Questions");
        formPanel.add(loadQuestionsButton);
        formPanel.add(new JLabel("Select questions below"));

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Question", "Type", "Difficulty", "Marks"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        questionTable = new JTable(tableModel);
        questionTable.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        );

        add(new JScrollPane(questionTable), BorderLayout.CENTER);

        JButton createButton = new JButton("Create Exam");
        JButton closeButton = new JButton("Close");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(createButton);
        bottomPanel.add(closeButton);

        add(bottomPanel, BorderLayout.SOUTH);

        loadSubjects();

        loadQuestionsButton.addActionListener(e -> loadQuestions());
        createButton.addActionListener(e -> createExam());
        closeButton.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void loadSubjects() {
        subjectCombo.removeAllItems();
        subjectIds.clear();

        String sql = "SELECT subject_id, subject_name FROM subjects";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                subjectIds.add(rs.getInt("subject_id"));
                subjectCombo.addItem(rs.getString("subject_name"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading subjects: " + e.getMessage());
        }
    }

    private void loadQuestions() {
        tableModel.setRowCount(0);

        int selectedIndex = subjectCombo.getSelectedIndex();

        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a subject.");
            return;
        }

        int subjectId = subjectIds.get(selectedIndex);

        String sql = """
                SELECT question_id, question_text,
                       question_type, difficulty, marks
                FROM questions
                WHERE subject_id = ?
                ORDER BY question_id
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subjectId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("question_id"),
                        rs.getString("question_text"),
                        rs.getString("question_type"),
                        rs.getString("difficulty"),
                        rs.getDouble("marks")
                });
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "No questions found for this subject.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading questions: " + e.getMessage());
        }
    }

    private void createExam() {
        int subjectIndex = subjectCombo.getSelectedIndex();

        if (subjectIndex == -1) {
            JOptionPane.showMessageDialog(this, "Select a subject.");
            return;
        }

        if (questionTable.getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one question.");
            return;
        }

        try {
            String title = titleField.getText().trim();
            int duration = Integer.parseInt(durationField.getText().trim());
            double passMarks = Double.parseDouble(
                    passMarksField.getText().trim());

            if (title.isEmpty() || duration <= 0 || passMarks < 0) {
                JOptionPane.showMessageDialog(this,
                        "Enter valid exam details.");
                return;
            }

            Timestamp startTime = Timestamp.valueOf(
                    startField.getText().trim());

            Timestamp endTime = Timestamp.valueOf(
                    endField.getText().trim());

            if (!endTime.after(startTime)) {
                JOptionPane.showMessageDialog(this,
                        "End time must be after start time.");
                return;
            }

            int subjectId = subjectIds.get(subjectIndex);

            List<Integer> questionIds = new ArrayList<>();
            double totalMarks = 0;

            for (int row : questionTable.getSelectedRows()) {
                questionIds.add(
                        Integer.parseInt(tableModel.getValueAt(row, 0).toString())
                );

                totalMarks += Double.parseDouble(
                        tableModel.getValueAt(row, 4).toString()
                );
            }

            if (passMarks > totalMarks) {
                JOptionPane.showMessageDialog(this,
                        "Pass marks cannot exceed total marks (" +
                                totalMarks + ").");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                try {
                    String examSql = """
                            INSERT INTO exams
                            (subject_id, title, duration_minutes,
                             total_marks, pass_marks, start_time,
                             end_time, created_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """;

                    int examId;

                    try (PreparedStatement ps = con.prepareStatement(
                            examSql, Statement.RETURN_GENERATED_KEYS)) {

                        ps.setInt(1, subjectId);
                        ps.setString(2, title);
                        ps.setInt(3, duration);
                        ps.setDouble(4, totalMarks);
                        ps.setDouble(5, passMarks);
                        ps.setTimestamp(6, startTime);
                        ps.setTimestamp(7, endTime);
                        ps.setInt(8, teacherId);

                        ps.executeUpdate();

                        ResultSet keys = ps.getGeneratedKeys();
                        keys.next();
                        examId = keys.getInt(1);
                    }

                    String questionSql = """
                            INSERT INTO exam_questions
                            (exam_id, question_id, question_order, marks)
                            VALUES (?, ?, ?, ?)
                            """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(questionSql)) {

                        int order = 1;

                        for (int row : questionTable.getSelectedRows()) {
                            int questionId = Integer.parseInt(
                                    tableModel.getValueAt(row, 0).toString());

                            double marks = Double.parseDouble(
                                    tableModel.getValueAt(row, 4).toString());

                            ps.setInt(1, examId);
                            ps.setInt(2, questionId);
                            ps.setInt(3, order++);
                            ps.setDouble(4, marks);

                            ps.addBatch();
                        }

                        ps.executeBatch();
                    }

                    con.commit();

                    JOptionPane.showMessageDialog(this,
                            "Exam created successfully!\nExam ID: " + examId);

                    dispose();

                } catch (SQLException e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Duration and pass marks must be valid numbers.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date/time format.\nUse: yyyy-MM-dd HH:mm:ss");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage());
        }
    }
}
