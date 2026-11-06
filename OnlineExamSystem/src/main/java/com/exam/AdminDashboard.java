
package com.exam;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {

    private JTable userTable;
    private DefaultTableModel model;

    public AdminDashboard() {

        setTitle("Admin Dashboard - Online Examination System");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel("ADMIN DASHBOARD",
                SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton addButton = new JButton("Add User");
        JButton refreshButton = new JButton("Refresh Users");
        JButton logoutButton = new JButton("Logout");

        model = new DefaultTableModel(
                new String[]{"ID", "Name", "Email", "Role"}, 0);

        userTable = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(userTable);

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);

        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addUser());
        refreshButton.addActionListener(e -> loadUsers());

        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        loadUsers();

        setVisible(true);
    }

    private void loadUsers() {

        model.setRowCount(0);

        String sql = "SELECT user_id, full_name, email, role FROM users";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role")
                });
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void addUser() {

        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        String[] roles = {"TEACHER", "STUDENT"};

        JComboBox<String> roleBox = new JComboBox<>(roles);

        Object[] fields = {
                "Full Name:", nameField,
                "Email:", emailField,
                "Password:", passwordField,
                "Role:", roleBox
        };

        int result = JOptionPane.showConfirmDialog(
                this, fields, "Add User",
                JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION)
            return;

        String sql = "INSERT INTO users "
                + "(full_name, email, password_hash, role) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nameField.getText());
            ps.setString(2, emailField.getText());
            ps.setString(3,
                    new String(passwordField.getPassword()));
            ps.setString(4, roleBox.getSelectedItem().toString());

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "User Added Successfully!");

            loadUsers();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage());
        }
    }
}
