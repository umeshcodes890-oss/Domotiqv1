package com.domotiq.gui;

import com.domotiq.controller.ActivityController;
import com.domotiq.controller.DeviceController;
import com.domotiq.controller.RoomController;
import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.User;
import com.domotiq.simulation.DeviceSimulator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final User user;
    private final DeviceController deviceController = new DeviceController();
    private final RoomController roomController = new RoomController();
    private final ActivityController activityController = new ActivityController();
    private final DeviceSimulator simulator;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private DashboardPanel dashboardPanel;
    private DevicesPanel devicesPanel;
    private RoomsPanel roomsPanel;
    private HistoryPanel historyPanel;

    public MainFrame(User user) {
        super("DOMOTIQ - " + user.getFullName());
        this.user = user;
        Theme.apply(ThemeManager.load(user.getUsername(), Theme.Mode.DARK));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(new Dimension(1320, 820));
        setLocationRelativeTo(null);

        try {
            deviceController.loadForUser(user.getId());
        } catch (SQLException ex) {
            showError("Could not load your devices: " + ex.getMessage());
        }

        simulator = new DeviceSimulator(deviceController::getDevices);
        buildUi();
        simulator.start();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);

        content.setBackground(Theme.BACKGROUND);
        dashboardPanel = new DashboardPanel(this);
        devicesPanel = new DevicesPanel(this);
        roomsPanel = new RoomsPanel(this);
        historyPanel = new HistoryPanel(this);
        content.add(dashboardPanel, "dashboard");
        content.add(devicesPanel, "devices");
        content.add(roomsPanel, "rooms");
        content.add(historyPanel, "history");
        right.add(content, BorderLayout.CENTER);

        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        simulator.setOnChange(d -> {
            dashboardPanel.onLiveUpdate();
            devicesPanel.onLiveUpdate();
        });

        refreshAll();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(210, 0));
        side.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

        JPanel logoRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoRow.setMaximumSize(new Dimension(178, 46));
        logoRow.add(new LoginFrame.LogoMark(30));
        logoRow.add(Theme.label("DOMOTIQ", 20, Font.BOLD, Theme.SIDEBAR_TEXT));
        side.add(logoRow);
        side.add(Box.createVerticalStrut(28));

        ButtonGroup group = new ButtonGroup();
        side.add(navButton(group, "Dashboard", "dashboard"));
        side.add(navButton(group, "Devices", "devices"));
        side.add(navButton(group, "Rooms", "rooms"));
        side.add(navButton(group, "History", "history"));

        side.add(Box.createVerticalStrut(20));
        JLabel note = Theme.label("Simulated IoT devices -\nno physical hardware attached.",
                11, Font.ITALIC, new Color(120, 132, 158));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(note);

        return side;
    }

    private JToggleButton navButton(ButtonGroup group, String text, String card) {
        JToggleButton b = new JToggleButton(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                if (isSelected()) {
                    var g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 12, 12);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.font(16, Font.PLAIN));
        b.setForeground(Theme.SIDEBAR_TEXT);
        b.setBackground(Theme.SIDEBAR_BUTTON);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(182, 54));
        b.setMaximumSize(new Dimension(182, 54));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMargin(new java.awt.Insets(0, 14, 0, 0));
        group.add(b);
        b.addActionListener(e -> {
            cards.show(content, card);
            onCardShown(card);
        });
        return b;
    }

    private JPanel buildHeader() {
        JPanel header = Theme.hRow(12);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));
        header.add(Theme.label("Welcome back, " + user.getFullName(),
                19, Font.BOLD, Theme.TEXT));

        var themeBtn = Theme.ghostButton(
                Theme.mode == Theme.Mode.DARK ? "Light mode" : "Dark mode", Theme.AMBER);
        var refreshBtn = Theme.ghostButton("Refresh", Theme.ACCENT);
        var logoutBtn = Theme.ghostButton("Log out", Theme.RED);
        JPanel rightButtons = Theme.hRow(10);
        rightButtons.add(themeBtn);
        rightButtons.add(refreshBtn);
        rightButtons.add(logoutBtn);
        rightButtons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(header, BorderLayout.WEST);
        bar.add(rightButtons, BorderLayout.EAST);

        themeBtn.addActionListener(e -> {
            Theme.Mode next = Theme.mode == Theme.Mode.DARK
                    ? Theme.Mode.LIGHT : Theme.Mode.DARK;
            ThemeManager.save(user.getUsername(), next);
            Theme.apply(next);
            simulator.stop();
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame(user).setVisible(true));
        });
        refreshBtn.addActionListener(e -> refreshAll());
        logoutBtn.addActionListener(e -> {
            simulator.stop();
            DatabaseConnection.close();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        return bar;
    }

    private void onCardShown(String card) {
        switch (card) {
            case "dashboard" -> dashboardPanel.refresh();
            case "devices" -> devicesPanel.refresh();
            case "rooms" -> roomsPanel.refresh();
            case "history" -> historyPanel.refresh();
            default -> { }
        }
    }

    public void refreshAll() {
        dashboardPanel.refresh();
        devicesPanel.refresh();
        roomsPanel.refresh();
        historyPanel.refresh();
    }

    /** Called after any device mutation so every view stays consistent. */
    public void onDataChanged() {
        refreshAll();
    }

    /* ---------- accessors for panels ---------- */

    public User getUser() { return user; }
    public DeviceController getDeviceController() { return deviceController; }
    public RoomController getRoomController() { return roomController; }
    public ActivityController getActivityController() { return activityController; }

    void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "DOMOTIQ", JOptionPane.ERROR_MESSAGE);
    }

    void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "DOMOTIQ",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
