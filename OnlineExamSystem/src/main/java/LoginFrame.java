
package com.exam;

import javax.swing.*;

import com.exam.AdminDashboard;
import com.exam.DBConnection;
import com.exam.StudentDashboard;
import com.exam.TeacherDashboard;

import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginFrame() {

        setTitle("Online Examination System");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Online Examination System",
                SwingConstants.CENTER);

        emailField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");

        panel.add(title);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        add(panel, BorderLayout.CENTER);
        add(loginButton, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> login());

        setVisible(true);
    }

    private void login() {

        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        String sql = "SELECT user_id, full_name, role FROM users "
                   + "WHERE email = ? AND password_hash = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                int userId = rs.getInt("user_id");
String name = rs.getString("full_name");
String role = rs.getString("role");

JOptionPane.showMessageDialog(this,
        "Welcome " + name + "\nRole: " + role);

openDashboard(role, name, userId);

            } else { 

                JOptionPane.showMessageDialog(this,
                        "Invalid Email or Password!");
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(this,
                    "Database Error: " + ex.getMessage());
        }
    }


private void openDashboard(String role, String name, int userId) {

    switch (role) {

        case "ADMIN":
            new AdminDashboard();
            break;

        case "TEACHER":
            new TeacherDashboard(userId, name);
            break;

        case "STUDENT":
            new StudentDashboard(userId, name);
            break;

        default:
            JOptionPane.showMessageDialog(this,
                    "Invalid user role.");
            return;
    }

    dispose();
}

}
