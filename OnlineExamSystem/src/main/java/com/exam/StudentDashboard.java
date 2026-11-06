
package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StudentDashboard extends JFrame {

    private final int studentId;
    private final String studentName;

    private JTable examTable;
    private DefaultTableModel model;

    public StudentDashboard(int studentId, String studentName) {

        this.studentId = studentId;
        this.studentName = studentName;

        setTitle("Student Dashboard");
        setSize(800, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel(
                "STUDENT DASHBOARD - " + studentName,
                SwingConstants.CENTER);

        title.setFont(new Font("Arial", Font.BOLD, 22));

        model = new DefaultTableModel(
                new String[]{"Exam ID", "Exam Title", "Subject",
                        "Duration", "Total Marks", "Start Time", "End Time"},
                0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        examTable = new JTable(model);

        JButton refresh = new JButton("Refresh Exams");
        JButton startExam = new JButton("Start Exam");
        JButton logout = new JButton("Logout");
        JButton resultsButton = new JButton("My Results");

        JPanel buttons = new JPanel();
        buttons.add(refresh);
        buttons.add(startExam);
        buttons.add(logout);
        buttons.add(resultsButton);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(examTable), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        refresh.addActionListener(e -> loadExams());

        startExam.addActionListener(e -> startExam());

        resultsButton.addActionListener(e ->
        new StudentResultsFrame(studentId, studentName));

        logout.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        loadExams();

        setVisible(true);
    }

    private void loadExams() {

        model.setRowCount(0);

        String sql = """
            SELECT e.exam_id, e.title, s.subject_name,
                   e.duration_minutes, e.total_marks,
                   e.start_time, e.end_time
            FROM exams e
            JOIN subjects s ON e.subject_id = s.subject_id
            WHERE NOW() BETWEEN e.start_time AND e.end_time
            ORDER BY e.start_time
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("exam_id"),
                        rs.getString("title"),
                        rs.getString("subject_name"),
                        rs.getInt("duration_minutes") + " minutes",
                        rs.getDouble("total_marks"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time")
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading exams: " + e.getMessage());
        }
    }

    private void startExam() {

        int row = examTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an exam.");
            return;
        }

        int examId = Integer.parseInt(
                model.getValueAt(row, 0).toString());

        String examTitle = model.getValueAt(row, 1).toString();

        int duration = Integer.parseInt(
                model.getValueAt(row, 3).toString()
                        .replace(" minutes", ""));

        new ExamFrame(examId, studentId, studentName,
                examTitle, duration);
    }
}
