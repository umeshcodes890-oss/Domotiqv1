package com.domotiq.gui;

import com.domotiq.controller.AuthController;
import com.domotiq.model.User;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.SQLException;

/** Split-brand login screen: animated DOMOTIQ mark on the left, form on the right. */
public class LoginFrame extends JFrame {

    private final AuthController authController = new AuthController();
    private final JTextField usernameField = Theme.textField(null, 0);
    private final JPasswordField passwordField = new JPasswordField();

    public LoginFrame() {
        super("DOMOTIQ - Sign in");
        Theme.apply(ThemeManager.loadDefault(Theme.Mode.DARK));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 540));
        setSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);

        BrandPanel brand = new BrandPanel();
        brand.setPreferredSize(new Dimension(420, 0));
        root.add(brand, BorderLayout.WEST);

        root.add(buildFormPanel(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        JPanel card = Theme.card();
        card.setBackground(Theme.PANEL);
        card.setLayout(new BorderLayout(0, 24));
        card.setBorder(BorderFactory.createEmptyBorder(36, 40, 36, 40));
        card.setPreferredSize(new Dimension(400, 400));

        JPanel title = new JPanel(new BorderLayout(0, 4));
        title.setOpaque(false);
        title.add(Theme.label("Welcome back", 24, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        title.add(Theme.label("Sign in to control your home", 13, Font.PLAIN, Theme.TEXT_MUTED),
                BorderLayout.SOUTH);

        stylePassword();
        JPanel form = new JPanel(new BorderLayout(0, 14));
        form.setOpaque(false);
        form.add(captioned("USERNAME", usernameField), BorderLayout.NORTH);
        form.add(captioned("PASSWORD", passwordField), BorderLayout.CENTER);

        var loginBtn = Theme.button("Sign in", Theme.ACCENT);
        loginBtn.setPreferredSize(new Dimension(0, 44));
        var registerBtn = Theme.ghostButton("Create an account", Theme.GREEN);
        registerBtn.setPreferredSize(new Dimension(0, 40));

        JPanel buttons = new JPanel(new BorderLayout(0, 8));
        buttons.setOpaque(false);
        buttons.add(loginBtn, BorderLayout.NORTH);
        buttons.add(registerBtn, BorderLayout.SOUTH);

        card.add(title, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        outer.add(card);

        loginBtn.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        registerBtn.addActionListener(e -> {
            RegistrationDialog dialog = new RegistrationDialog(this);
            dialog.setVisible(true);
            if (dialog.isRegistered()) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        return outer;
    }

    private void stylePassword() {
        passwordField.setFont(Theme.font(14, Font.PLAIN));
        passwordField.setForeground(Theme.TEXT);
        passwordField.setCaretColor(Theme.TEXT);
        passwordField.setBackground(Theme.FIELD_BG);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INPUT_BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    private static JPanel captioned(String caption, JComponent editor) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(Theme.label(caption, 11, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }

    /** Left panel: gradient backdrop, logo mark, wordmark, tagline, pulsing orbs. */
    private static final class BrandPanel extends JPanel {
        private double pulse;

        BrandPanel() {
            setLayout(new GridBagLayout());

            JPanel stack = new JPanel(new BorderLayout(0, 26));
            stack.setOpaque(false);
            stack.setPreferredSize(new Dimension(340, 210));
            stack.setMaximumSize(new Dimension(340, 210));

            JPanel markRow = Theme.hRow(14);
            markRow.add(new LogoMark(56));
            markRow.add(Theme.label("DOMOTIQ", 30, Font.BOLD, Color.WHITE));

            JLabel tagline = Theme.label(
                    "<html>One command center for every corner<br>of your home: lights, climate,<br>security and sensors.</html>",
                    14, Font.PLAIN, new Color(175, 187, 210));

            stack.add(markRow, BorderLayout.NORTH);
            stack.add(tagline, BorderLayout.CENTER);
            stack.add(Theme.label("Simulated IoT devices v1.0",
                    12, Font.ITALIC, new Color(125, 137, 162)), BorderLayout.SOUTH);
            add(stack);

            Timer t = new Timer(40, e -> {
                pulse = (pulse + 0.02) % (2 * Math.PI);
                repaint();
            });
            t.setCoalesce(true);
            t.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, new Color(16, 22, 38),
                    getWidth(), getHeight(), new Color(30, 46, 80)));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int glow = (int) (26 + 14 * Math.sin(pulse));
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(),
                    Theme.ACCENT.getBlue(), glow));
            g2.fillOval(-90, getHeight() - 230, 300, 300);
            g2.fillOval(getWidth() - 120, -110 + (int) (8 * Math.sin(pulse)), 240, 240);
            g2.dispose();
        }
    }

    /** Rounded gradient tile with a house + signal glyph. */
    static final class LogoMark extends JComponent {
        private final int size;

        LogoMark(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT,
                    size, size, Theme.ACCENT_DARK));
            g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);

            g2.setColor(Color.WHITE);
            int u = size / 16;
            int[] xs = {3 * u, 8 * u, 13 * u};
            int[] ys = {9 * u, 4 * u, 9 * u};
            g2.fillPolygon(xs, ys, 3);
            g2.fillRoundRect(5 * u, 9 * u, 6 * u, 5 * u, u, u);
            g2.setStroke(new BasicStroke(Math.max(1.5f, u * 0.7f)));
            g2.drawArc(10 * u, 9 * u, 4 * u, 4 * u, 0, 90);
            g2.drawArc(11 * u, 10 * u, 3 * u, 3 * u, 0, 90);
            g2.dispose();
        }
    }

    private void attemptLogin() {
        String user = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (user.isEmpty() || password.isEmpty()) {
            error("Please enter both username and password.");
            return;
        }
        try {
            User authenticated = authController.login(user, password);
            if (authenticated == null) {
                error("Invalid username or password.");
                return;
            }
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(authenticated);
                mainFrame.setVisible(true);
                dispose();
            });
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "DOMOTIQ", JOptionPane.ERROR_MESSAGE);
    }
}
