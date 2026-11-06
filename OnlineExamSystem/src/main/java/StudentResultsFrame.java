
package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StudentResultsFrame extends JFrame {

    private final int studentId;
    private JTable resultTable;
    private DefaultTableModel model;

    public StudentResultsFrame(int studentId, String studentName) {

        this.studentId = studentId;

        setTitle("My Exam Results");
        setSize(850, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel(
                "EXAM RESULTS - " + studentName,
                SwingConstants.CENTER);

        title.setFont(new Font("Arial", Font.BOLD, 22));

        model = new DefaultTableModel(
                new String[]{"Exam", "Subject", "Score",
                        "Total Marks", "Pass Marks", "Result"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(model);

        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadResults());
        closeButton.addActionListener(e -> dispose());

        loadResults();

        setVisible(true);
    }

    private void loadResults() {

        model.setRowCount(0);

        String sql = """
            SELECT e.title, s.subject_name,
                   a.score, e.total_marks, e.pass_marks
            FROM attempts a
            JOIN exams e ON a.exam_id = e.exam_id
            JOIN subjects s ON e.subject_id = s.subject_id
            WHERE a.student_id = ?
              AND a.status = 'EVALUATED'
            ORDER BY a.submitted_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                double score = rs.getDouble("score");
                double totalMarks = rs.getDouble("total_marks");
                double passMarks = rs.getDouble("pass_marks");

                String result = score >= passMarks ? "PASS" : "FAIL";

                model.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("subject_name"),
                        score,
                        totalMarks,
                        passMarks,
                        result
                });
            }

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "No completed exam results found.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading results: " + e.getMessage());
        }
    }
}
