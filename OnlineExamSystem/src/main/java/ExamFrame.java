
package com.exam;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class ExamFrame extends JFrame {

    private int examId, studentId, attemptId;
    private String studentName;
    private int remainingSeconds;

    private JLabel timerLabel;
    private JPanel questionsPanel;

    private List<QuestionData> questions = new ArrayList<>();
    private Map<Integer, ButtonGroup> answerGroups = new HashMap<>();
    private Map<Integer, Integer> optionQuestionMap = new HashMap<>();

    private javax.swing.Timer timer;

    static class QuestionData {
        int id;
        String text;
        double marks;
        List<OptionData> options = new ArrayList<>();
    }

    static class OptionData {
        int id;
        String text;
    }

    public ExamFrame(int examId, int studentId, String studentName,
                     String examTitle, int duration) {

        this.examId = examId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.remainingSeconds = duration * 60;

        setTitle("Exam: " + examTitle);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        timerLabel = new JLabel("", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));

        questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(
                questionsPanel, BoxLayout.Y_AXIS));

        JButton submitButton = new JButton("Submit Exam");

        add(timerLabel, BorderLayout.NORTH);
        add(new JScrollPane(questionsPanel), BorderLayout.CENTER);
        add(submitButton, BorderLayout.SOUTH);

        submitButton.addActionListener(e -> submitExam());

        if (createAttempt() && loadQuestions()) {
            setVisible(true);
            startTimer();
        } else {
            dispose();
        }
    }

    private boolean createAttempt() {

        String sql = """
            INSERT INTO attempts (exam_id, student_id, status)
            VALUES (?, ?, 'IN_PROGRESS')
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, examId);
            ps.setInt(2, studentId);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();

            attemptId = keys.getInt(1);
            return true;

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this,
                    "Could not start exam. You may have already attempted it.\n"
                            + e.getMessage());
            return false;
        }
    }

    private boolean loadQuestions() {

        String sql = """
            SELECT q.question_id, q.question_text, eq.marks
            FROM exam_questions eq
            JOIN questions q ON eq.question_id = q.question_id
            WHERE eq.exam_id = ?
            ORDER BY eq.question_order
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, examId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                QuestionData q = new QuestionData();
                q.id = rs.getInt("question_id");
                q.text = rs.getString("question_text");
                q.marks = rs.getDouble("marks");

                questions.add(q);
            }

            for (QuestionData q : questions) {
                loadOptions(q);
                displayQuestion(q);
            }

            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "This exam has no questions.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return false;
        }
    }

    private void loadOptions(QuestionData q) throws SQLException {

        String sql = """
            SELECT option_id, option_text
            FROM options
            WHERE question_id = ?
            ORDER BY option_id
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, q.id);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                OptionData option = new OptionData();
                option.id = rs.getInt("option_id");
                option.text = rs.getString("option_text");

                q.options.add(option);
            }
        }
    }

    private void displayQuestion(QuestionData q) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                "Question - " + q.marks + " marks"));

        panel.add(new JLabel(q.text));

        ButtonGroup group = new ButtonGroup();
        answerGroups.put(q.id, group);

        for (OptionData option : q.options) {

            JRadioButton radio = new JRadioButton(option.text);
            radio.setActionCommand(String.valueOf(option.id));

            group.add(radio);
            panel.add(radio);
        }

        questionsPanel.add(panel);
        questionsPanel.add(Box.createVerticalStrut(10));
    }

    private void startTimer() {

        updateTimerLabel();

        timer = new javax.swing.Timer(1000, e -> {

            remainingSeconds--;

            updateTimerLabel();

            if (remainingSeconds <= 0) {
                timer.stop();

                JOptionPane.showMessageDialog(this,
                        "Time is over. Your exam will be submitted.");

                submitExam();
            }
        });

        timer.start();
    }

    private void updateTimerLabel() {

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        timerLabel.setText(String.format(
                "Time Remaining: %02d:%02d", minutes, seconds));
    }

    private void submitExam() {

        if (timer != null) {
            timer.stop();
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to submit?",
                "Submit Exam", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {

            if (timer != null && remainingSeconds > 0) {
                timer.start();
            }

            return;
        }

        double score = 0;

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                String answerSQL = """
                    INSERT INTO answers
                    (attempt_id, question_id, selected_option_id,
                     is_correct, awarded_marks)
                    VALUES (?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(answerSQL)) {

                    for (QuestionData q : questions) {

                        Integer selectedOption = null;
                        boolean correct = false;
                        double awarded = 0;

                        ButtonGroup group = answerGroups.get(q.id);

                        if (group != null && group.getSelection() != null) {

                            selectedOption = Integer.parseInt(
                                    group.getSelection().getActionCommand());

                            correct = checkCorrectOption(
                                    con, q.id, selectedOption);

                            if (correct) {
                                awarded = q.marks;
                                score += q.marks;
                            }
                        }

                        ps.setInt(1, attemptId);
                        ps.setInt(2, q.id);

                        if (selectedOption == null) {
                            ps.setNull(3, Types.INTEGER);
                        } else {
                            ps.setInt(3, selectedOption);
                        }

                        ps.setBoolean(4, correct);
                        ps.setDouble(5, awarded);

                        ps.executeUpdate();
                    }
                }

                String updateSQL = """
                    UPDATE attempts
                    SET score = ?, submitted_at = NOW(),
                        status = 'EVALUATED'
                    WHERE attempt_id = ?
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(updateSQL)) {

                    ps.setDouble(1, score);
                    ps.setInt(2, attemptId);
                    ps.executeUpdate();
                }

                con.commit();

                JOptionPane.showMessageDialog(this,
                        "Exam submitted successfully!\n"
                                + "Your score: " + score);

                dispose();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this,
                    "Submission error: " + e.getMessage());
        }
    }

    private boolean checkCorrectOption(Connection con,
                                       int questionId,
                                       int optionId)
            throws SQLException {

        String sql = """
            SELECT is_correct
            FROM options
            WHERE question_id = ? AND option_id = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, questionId);
            ps.setInt(2, optionId);

            ResultSet rs = ps.executeQuery();

            return rs.next() && rs.getBoolean("is_correct");
        }
    }
}
