package com.domotiq.gui;

import com.domotiq.automation.RuleEngine;
import com.domotiq.controller.ActivityController;
import com.domotiq.controller.AutomationController;
import com.domotiq.controller.DeviceController;
import com.domotiq.controller.EnergyController;
import com.domotiq.controller.RoomController;
import com.domotiq.db.DatabaseConnection;
import com.domotiq.model.User;
import com.domotiq.simulation.DeviceSimulator;
import com.domotiq.simulation.EnergyTracker;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final User user;
    private final DeviceController deviceController = new DeviceController();
    private final RoomController roomController = new RoomController();
    private final ActivityController activityController = new ActivityController();
    private final AutomationController automationController = new AutomationController();
    private final EnergyController energyController = new EnergyController();
    private final RuleEngine ruleEngine = new RuleEngine(deviceController, automationController);
    private final EnergyTracker energyTracker = new EnergyTracker();
    private final javax.swing.Timer minuteTimer;
    private final DeviceSimulator simulator;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private Sidebar sidebar;
    private DashboardPanel dashboardPanel;
    private DevicesPanel devicesPanel;
    private RoomsPanel roomsPanel;
    private HistoryPanel historyPanel;
    private AutomationsPanel automationsPanel;
    private EnergyPanel energyPanel;

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
            ruleEngine.loadForUser(user.getId());
        } catch (SQLException ex) {
            showError("Could not load your devices: " + ex.getMessage());
        }

        simulator = new DeviceSimulator(deviceController::getDevices);
        simulator.setEnergyTracker(energyTracker);
        simulator.setOnSensorReading(ruleEngine::onSensorReading);
        minuteTimer = new javax.swing.Timer(60_000, e -> onMinute());
        buildUi();
        simulator.start();
        minuteTimer.setInitialDelay(60_000);
        minuteTimer.start();
    }

    /** Scheduler tick: due automations fire and accumulated kWh is persisted. */
    private void onMinute() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        try {
            ruleEngine.onMinuteTick(now);
            energyTracker.flush(energyController.getRepository(), user.getId());
        } catch (SQLException ex) {
            showError("Energy/automation bookkeeping failed: " + ex.getMessage());
        }
        dashboardPanel.refresh();
        energyPanel.refresh();
        automationsPanel.refresh();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                var g2 = (java.awt.Graphics2D) g.create();
                g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.BG_TOP,
                        0, getHeight(), Theme.BG_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        sidebar = new Sidebar(this);
        root.add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);

        content.setOpaque(false);
        dashboardPanel = new DashboardPanel(this);
        devicesPanel = new DevicesPanel(this);
        roomsPanel = new RoomsPanel(this);
        historyPanel = new HistoryPanel(this);
        automationsPanel = new AutomationsPanel(this);
        energyPanel = new EnergyPanel(this);
        content.add(dashboardPanel, "dashboard");
        content.add(devicesPanel, "devices");
        content.add(roomsPanel, "rooms");
        content.add(automationsPanel, "automations");
        content.add(energyPanel, "energy");
        content.add(historyPanel, "history");
        right.add(content, BorderLayout.CENTER);

        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        simulator.setOnChange(d -> {
            dashboardPanel.onLiveUpdate();
            devicesPanel.onLiveUpdate();
        });

        refreshAll();
        sidebar.selectIndex(0);
        installShortcuts();
    }

    /** Selects a rail entry by index, used for Alt+number and the startup default. */
    void showScreen(int index) {
        sidebar.selectIndex(index);
    }

    /** Swaps the visible card and refreshes it, so a screen is never shown stale. */
    public void showCard(String card) {
        cards.show(content, card);
        onCardShown(card);
    }

    /** Alt+1..6 jump between screens, Ctrl+B folds the rail. */
    private void installShortcuts() {
        InputMap keys = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actions = getRootPane().getActionMap();
        for (int i = 0; i < Sidebar.screenCount(); i++) {
            final int index = i;
            String key = "nav" + index;
            keys.put(KeyStroke.getKeyStroke("alt " + (index + 1)), key);
            actions.put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showScreen(index);
                }
            });
        }
        keys.put(KeyStroke.getKeyStroke("control B"), "foldRail");
        actions.put("foldRail", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sidebar.toggleCollapsed();
            }
        });
    }

    /** The palette lives in static fields, so switching it rebuilds the window. */
    void toggleTheme() {
        Theme.Mode next = Theme.mode == Theme.Mode.DARK
                ? Theme.Mode.LIGHT : Theme.Mode.DARK;
        ThemeManager.save(user.getUsername(), next);
        Theme.apply(next);
        shutdownTimers();
        dispose();
        SwingUtilities.invokeLater(() -> new MainFrame(user).setVisible(true));
    }

    private JPanel buildHeader() {
        JPanel header = Theme.hRow(12);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));
        header.add(Theme.label("Welcome back, " + user.getFullName(),
                19, Font.BOLD, Theme.TEXT));

        var refreshBtn = Theme.ghostButton("Refresh", Theme.ACCENT);
        var logoutBtn = Theme.ghostButton("Log out", Theme.RED);
        JPanel rightButtons = Theme.hRow(10);
        rightButtons.add(refreshBtn);
        rightButtons.add(logoutBtn);
        rightButtons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(header, BorderLayout.WEST);
        bar.add(rightButtons, BorderLayout.EAST);

        refreshBtn.addActionListener(e -> refreshAll());
        logoutBtn.addActionListener(e -> {
            shutdownTimers();
            DatabaseConnection.close();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        return bar;
    }

    private void shutdownTimers() {
        simulator.stop();
        minuteTimer.stop();
        try {
            energyTracker.flush(energyController.getRepository(), user.getId());
        } catch (SQLException ignored) {
            // a lost final minute of estimates is acceptable on shutdown
        }
    }

    private void onCardShown(String card) {
        switch (card) {
            case "dashboard" -> dashboardPanel.refresh();
            case "devices" -> devicesPanel.refresh();
            case "rooms" -> roomsPanel.refresh();
            case "automations" -> automationsPanel.refresh();
            case "energy" -> energyPanel.refresh();
            case "history" -> historyPanel.refresh();
            default -> { }
        }
    }

    public void reloadAutomations() {
        try {
            ruleEngine.loadForUser(user.getId());
        } catch (SQLException ex) {
            showError("Could not reload automations: " + ex.getMessage());
        }
    }

    public void refreshAll() {
        dashboardPanel.refresh();
        devicesPanel.refresh();
        roomsPanel.refresh();
        automationsPanel.refresh();
        energyPanel.refresh();
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
    public AutomationController getAutomationController() { return automationController; }
    public EnergyController getEnergyController() { return energyController; }
    public RuleEngine getRuleEngine() { return ruleEngine; }

    void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "DOMOTIQ", JOptionPane.ERROR_MESSAGE);
    }

    void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "DOMOTIQ",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
