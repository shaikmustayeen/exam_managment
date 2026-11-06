
package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TeacherResultsFrame extends JFrame {

    private final int teacherId;
    private JTable resultsTable;
    private DefaultTableModel model;

    public TeacherResultsFrame(int teacherId, String teacherName) {

        this.teacherId = teacherId;

        setTitle("Teacher Results Dashboard");
        setSize(1000, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel(
                "STUDENT RESULTS - " + teacherName,
                SwingConstants.CENTER);

        title.setFont(new Font("Arial", Font.BOLD, 22));

        model = new DefaultTableModel(
                new String[]{"Exam", "Student", "Student ID",
                        "Score", "Total Marks", "Pass Marks",
                        "Status", "Rank"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(model);

        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadResults());
        closeButton.addActionListener(e -> dispose());

        loadResults();

        setVisible(true);
    }

    private void loadResults() {

        model.setRowCount(0);

        String sql = """
            SELECT
                e.title AS exam_title,
                u.full_name AS student_name,
                u.user_id AS student_id,
                a.score,
                e.total_marks,
                e.pass_marks,
                CASE
                    WHEN a.score >= e.pass_marks THEN 'PASS'
                    ELSE 'FAIL'
                END AS result_status,
                RANK() OVER (
                    PARTITION BY e.exam_id
                    ORDER BY a.score DESC
                ) AS student_rank
            FROM attempts a
            JOIN exams e ON a.exam_id = e.exam_id
            JOIN users u ON a.student_id = u.user_id
            WHERE e.created_by = ?
              AND a.status = 'EVALUATED'
            ORDER BY e.exam_id, student_rank
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, teacherId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getString("exam_title"),
                        rs.getString("student_name"),
                        rs.getInt("student_id"),
                        rs.getDouble("score"),
                        rs.getDouble("total_marks"),
                        rs.getDouble("pass_marks"),
                        rs.getString("result_status"),
                        rs.getInt("student_rank")
                });
            }

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "No evaluated student results available yet.");
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this,
                    "Error loading results: " + e.getMessage());
        }
    }
}
