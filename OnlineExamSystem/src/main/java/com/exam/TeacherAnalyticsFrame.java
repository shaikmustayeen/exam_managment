package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TeacherAnalyticsFrame extends JFrame {

    private final int teacherId;
    private final String teacherName;

    private JLabel totalExamsLabel;
    private JLabel totalAttemptsLabel;
    private JLabel averageScoreLabel;
    private JLabel highestScoreLabel;
    private JLabel passPercentageLabel;

    private JTable analyticsTable;
    private DefaultTableModel tableModel;

    public TeacherAnalyticsFrame(int teacherId, String teacherName) {

        this.teacherId = teacherId;
        this.teacherName = teacherName;

        setTitle("Teacher Analytics - " + teacherName);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel title = new JLabel(
                "EXAM ANALYTICS - " + teacherName,
                SwingConstants.CENTER
        );

        title.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        summaryPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 10, 15, 10));

        totalExamsLabel = createMetricLabel("Total Exams");
        totalAttemptsLabel = createMetricLabel("Attempts");
        averageScoreLabel = createMetricLabel("Average Score");
        highestScoreLabel = createMetricLabel("Highest Score");
        passPercentageLabel = createMetricLabel("Pass Percentage");

        summaryPanel.add(totalExamsLabel);
        summaryPanel.add(totalAttemptsLabel);
        summaryPanel.add(averageScoreLabel);
        summaryPanel.add(highestScoreLabel);
        summaryPanel.add(passPercentageLabel);

        tableModel = new DefaultTableModel(
                new String[]{
                        "Exam ID",
                        "Exam Name",
                        "Attempts",
                        "Average Score",
                        "Highest Score",
                        "Passed",
                        "Pass Percentage"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        analyticsTable = new JTable(tableModel);
        analyticsTable.setRowHeight(25);

        JButton refreshButton = new JButton("Refresh Analytics");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        refreshButton.addActionListener(e -> loadAnalytics());

        closeButton.addActionListener(e -> dispose());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(summaryPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(analyticsTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        loadAnalytics();

        setVisible(true);
    }

    private JLabel createMetricLabel(String name) {

        JLabel label = new JLabel(
                "<html><center>" + name + "<br>Loading...</center></html>",
                SwingConstants.CENTER
        );

        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.setOpaque(true);
        label.setBackground(new Color(235, 245, 255));

        return label;
    }

    private void loadAnalytics() {

        loadSummary();
        loadExamWiseAnalytics();
    }

    private void loadSummary() {

        String sql = """
            SELECT
                COUNT(DISTINCT e.exam_id) AS total_exams,
                COUNT(a.attempt_id) AS total_attempts,
                AVG(a.score) AS average_score,
                MAX(a.score) AS highest_score,

                COALESCE(
                    100.0 * SUM(
                        CASE
                            WHEN a.attempt_id IS NOT NULL
                                 AND a.score >= e.pass_marks
                            THEN 1
                            ELSE 0
                        END
                    ) / NULLIF(COUNT(a.attempt_id), 0),
                    0
                ) AS pass_percentage

            FROM exams e

            LEFT JOIN attempts a
                ON e.exam_id = a.exam_id
                AND a.status IN ('SUBMITTED', 'EVALUATED')

            WHERE e.created_by = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, teacherId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    totalExamsLabel.setText(
                            "<html><center>Total Exams<br>"
                                    + rs.getInt("total_exams")
                                    + "</center></html>");

                    totalAttemptsLabel.setText(
                            "<html><center>Total Attempts<br>"
                                    + rs.getInt("total_attempts")
                                    + "</center></html>");

                    double average = rs.getDouble("average_score");

                    averageScoreLabel.setText(
                            "<html><center>Average Score<br>"
                                    + (rs.wasNull()
                                    ? "N/A"
                                    : String.format("%.2f", average))
                                    + "</center></html>");

                    double highest = rs.getDouble("highest_score");

                    highestScoreLabel.setText(
                            "<html><center>Highest Score<br>"
                                    + (rs.wasNull()
                                    ? "N/A"
                                    : String.format("%.2f", highest))
                                    + "</center></html>");

                    passPercentageLabel.setText(
                            "<html><center>Pass Percentage<br>"
                                    + String.format(
                                    "%.2f%%",
                                    rs.getDouble("pass_percentage"))
                                    + "</center></html>");
                }
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Error loading summary: " + e.getMessage()
            );
        }
    }

    private void loadExamWiseAnalytics() {

        tableModel.setRowCount(0);

        String sql = """
            SELECT
                e.exam_id,
                e.title,
                COUNT(a.attempt_id) AS total_attempts,
                AVG(a.score) AS average_score,
                MAX(a.score) AS highest_score,

                SUM(
                    CASE
                        WHEN a.attempt_id IS NOT NULL
                             AND a.score >= e.pass_marks
                        THEN 1
                        ELSE 0
                    END
                ) AS passed,

                COALESCE(
                    100.0 * SUM(
                        CASE
                            WHEN a.attempt_id IS NOT NULL
                                 AND a.score >= e.pass_marks
                            THEN 1
                            ELSE 0
                        END
                    ) / NULLIF(COUNT(a.attempt_id), 0),
                    0
                ) AS pass_percentage

            FROM exams e

            LEFT JOIN attempts a
                ON e.exam_id = a.exam_id
                AND a.status IN ('SUBMITTED', 'EVALUATED')

            WHERE e.created_by = ?

            GROUP BY e.exam_id, e.title, e.pass_marks

            ORDER BY e.exam_id DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, teacherId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    double average = rs.getDouble("average_score");
                    boolean averageIsNull = rs.wasNull();

                    double highest = rs.getDouble("highest_score");
                    boolean highestIsNull = rs.wasNull();

                    tableModel.addRow(new Object[]{
                            rs.getInt("exam_id"),
                            rs.getString("title"),
                            rs.getInt("total_attempts"),
                            averageIsNull
                                    ? "N/A"
                                    : String.format("%.2f", average),
                            highestIsNull
                                    ? "N/A"
                                    : String.format("%.2f", highest),
                            rs.getInt("passed"),
                            String.format(
                                    "%.2f%%",
                                    rs.getDouble("pass_percentage"))
                    });
                }
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Error loading exam analytics: " + e.getMessage()
            );
        }
    }
}