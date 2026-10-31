package com.domotiq.gui;

import com.domotiq.controller.AuthController;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;

public class RegistrationDialog extends JDialog {

    private final AuthController authController = new AuthController();
    private boolean registered;

    public RegistrationDialog(java.awt.Frame parent) {
        super(parent, "Create your DOMOTIQ account", true);
        buildUi();
        pack();
        setSize(new Dimension(460, getPreferredSize().height + 20));
        setLocationRelativeTo(parent);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        root.add(Theme.label("Register", 26, Font.BOLD, Theme.ACCENT), BorderLayout.NORTH);

        JTextField fullName = Theme.textField(null, 0);
        JTextField email = Theme.textField(null, 0);
        JTextField username = Theme.textField(null, 0);
        JPasswordField password = secure();
        JPasswordField confirm = secure();

        JPanel form = new JPanel(new java.awt.GridLayout(0, 1, 0, 12));
        form.setOpaque(false);
        form.add(captioned("FULL NAME", fullName));
        form.add(captioned("EMAIL", email));
        form.add(captioned("USERNAME", username));
        form.add(captioned("PASSWORD (min 8 chars)", password));
        form.add(captioned("CONFIRM PASSWORD", confirm));

        var registerBtn = Theme.button("Register", Theme.GREEN);
        registerBtn.setPreferredSize(new Dimension(0, 42));
        var cancelBtn = Theme.ghostButton("Cancel", Theme.TEXT_MUTED);
        cancelBtn.setPreferredSize(new Dimension(0, 42));
        JPanel buttons = new JPanel(new BorderLayout(0, 8));
        buttons.setOpaque(false);
        buttons.add(registerBtn, BorderLayout.NORTH);
        buttons.add(cancelBtn, BorderLayout.SOUTH);

        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        Runnable submit = () -> attemptRegister(fullName, email, username, password, confirm);
        registerBtn.addActionListener(e -> submit.run());
        cancelBtn.addActionListener(e -> dispose());
        for (JComponent f : new JComponent[]{fullName, email, username, password, confirm}) {
            if (f instanceof JTextField tf) {
                tf.addActionListener(e -> submit.run());
            }
        }
    }

    private static JPasswordField secure() {
        JPasswordField p = new JPasswordField();
        p.setFont(Theme.font(14, Font.PLAIN));
        p.setForeground(Theme.TEXT);
        p.setCaretColor(Theme.TEXT);
        p.setBackground(Theme.FIELD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return p;
    }

    private static JPanel captioned(String caption, JComponent editor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }

    private void attemptRegister(JTextField fullName, JTextField email, JTextField username,
                                 JPasswordField password, JPasswordField confirm) {
        try {
            authController.register(username.getText(), fullName.getText(), email.getText(),
                    new String(password.getPassword()), new String(confirm.getPassword()));
            registered = true;
            JOptionPane.showMessageDialog(this,
                    "Account created. You can now sign in.",
                    "Welcome to DOMOTIQ", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "DOMOTIQ",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "DOMOTIQ", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isRegistered() {
        return registered;
    }
}
