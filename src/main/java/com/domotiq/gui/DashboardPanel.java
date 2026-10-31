package com.domotiq.gui;

import com.domotiq.model.ActivityLog;
import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Overview: live stats, category counts, quick controls, recent activity. */
public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private final MainFrame main;
    private final JLabel totalValue = Theme.label("0", 30, Font.BOLD, Theme.ACCENT);
    private final JLabel onlineValue = Theme.label("0", 30, Font.BOLD, Theme.GREEN);
    private final JLabel offlineValue = Theme.label("0", 30, Font.BOLD, Theme.RED);
    private final JLabel activeValue = Theme.label("0", 30, Font.BOLD, Theme.AMBER);
    private final JLabel automationsValue = Theme.label("0", 30, Font.BOLD,
            new Color(120, 200, 255));
    private final JPanel categoryRow = Theme.hRow(10);
    private final JPanel quickControls = new JPanel(new java.awt.FlowLayout(
            java.awt.FlowLayout.LEFT, 14, 14));
    private final JPanel stats = new JPanel(new GridLayout(0, 5, 16, 0));
    private final JTable activityTable =
            UiTables.styledTable(new String[]{"When", "Device", "Action", "From", "To"}, 0);

    public DashboardPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        stats.setOpaque(false);
        stats.add(statCard("Registered devices", totalValue));
        stats.add(statCard("Online", onlineValue));
        stats.add(statCard("Offline", offlineValue));
        stats.add(statCard("Active (ON / LOCKED)", activeValue));
        stats.add(statCard("Automations fired today", automationsValue));

        // adaptive stat row: 3+2 once the window gets narrow
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int wanted = getWidth() < 900 ? 2 : 5;
                if (((GridLayout) stats.getLayout()).getColumns() != wanted) {
                    stats.setLayout(new GridLayout(0, wanted, 16, 16));
                    stats.revalidate();
                }
            }
        });

        JPanel categories = Theme.card();
        categories.setLayout(new BorderLayout(0, 8));
        categories.add(Theme.label("Device categories", 14, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        categoryRow.setOpaque(false);
        categories.add(categoryRow, BorderLayout.CENTER);
        categories.setPreferredSize(new Dimension(0, 96));

        JPanel quickSection = new JPanel(new BorderLayout(0, 10));
        quickSection.setOpaque(false);
        quickSection.add(Theme.label("Quick controls", 14, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        quickControls.setOpaque(false);
        JScrollPane quickScroll = scroll(quickControls);
        quickScroll.setPreferredSize(new Dimension(0, 470));
        quickScroll.setMaximumSize(new Dimension(0, 470));
        quickSection.add(quickScroll, BorderLayout.CENTER);

        JPanel historySection = Theme.card();
        historySection.setLayout(new BorderLayout(0, 10));
        historySection.add(Theme.label("Recent activity", 14, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        activityTable.setPreferredScrollableViewportSize(new Dimension(600, 170));
        historySection.add(scroll(activityTable), BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
        for (JPanel p : List.of(categories, quickSection, historySection)) {
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
            center.add(p);
        }

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setOpaque(false);
        root.add(stats, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private JPanel statCard(String caption, JLabel value) {
        JPanel c = Theme.card();
        c.setLayout(new BorderLayout());
        c.add(value, BorderLayout.NORTH);
        c.add(Theme.label(caption, 12, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.SOUTH);
        c.setPreferredSize(new Dimension(240, 100));
        return c;
    }

    private JScrollPane scroll(java.awt.Component view) {
        JScrollPane s = new JScrollPane(view);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getViewport().setOpaque(true);
        s.getViewport().setBackground(Theme.PANEL);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }

    public void onLiveUpdate() {
        refresh();
    }

    public void refresh() {
        var controller = main.getDeviceController();
        totalValue.setText(String.valueOf(controller.countAll()));
        onlineValue.setText(String.valueOf(controller.countOnline()));
        offlineValue.setText(String.valueOf(controller.countOffline()));
        activeValue.setText(String.valueOf(controller.countActive()));
        try {
            automationsValue.setText(String.valueOf(main.getAutomationController()
                    .countFiredToday(main.getUser().getId())));
        } catch (SQLException ex) {
            automationsValue.setText("—");
        }

        categoryRow.removeAll();
        Map<DeviceType, Long> counts = controller.countByType();
        for (DeviceType type : DeviceType.values()) {
            JLabel chip = Theme.label(type.getLabel() + ": " + counts.get(type),
                    13, Font.BOLD, counts.get(type) > 0 ? Theme.TEXT : Theme.TEXT_MUTED);
            JPanel chipCard = Theme.hRow(4);
            chipCard.setBackground(Theme.PANEL_HOVER);
            chipCard.add(Theme.statusDot(colorFor(type)));
            chipCard.add(chip);
            categoryRow.add(chipCard);
        }
        categoryRow.revalidate();
        categoryRow.repaint();

        quickControls.removeAll();
        List<Room> rooms;
        try {
            rooms = main.getRoomController().listRooms(main.getUser().getId());
        } catch (SQLException ex) {
            rooms = List.of();
        }
        List<Device> devices = controller.getDevices();
        if (devices.isEmpty()) {
            JPanel empty = Theme.card();
            empty.add(Theme.label("Add your first simulated device from the Devices screen.",
                    13, Font.PLAIN, Theme.TEXT_MUTED));
            quickControls.add(empty);
        } else {
            for (Device device : devices.stream().limit(9).toList()) {
                quickControls.add(new DeviceCard(main, device, rooms, false));
            }
        }
        quickControls.revalidate();
        quickControls.repaint();

        refreshActivityTable();
    }

    private void refreshActivityTable() {
        var model = UiTables.modelOf(activityTable);
        model.setRowCount(0);
        try {
            List<ActivityLog> logs =
                    main.getActivityController().recent(main.getUser().getId(), 8);
            for (ActivityLog log : logs) {
                String action = prettyAction(log.getAction());
                if ("AUTOMATION".equals(log.getActor())) {
                    action = "⚡ " + action + " (auto)";
                }
                model.addRow(new Object[]{
                        log.getPerformedAt() == null ? "" : TIME_FMT.format(log.getPerformedAt()),
                        log.getDeviceName(),
                        action,
                        dash(log.getPreviousStatus()),
                        dash(log.getNewStatus())});
            }
        } catch (SQLException ex) {
            main.showError("Could not load activity: " + ex.getMessage());
        }
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    static String prettyAction(String action) {
        if (action == null) return "";
        return switch (action) {
            case "ADDED" -> "Registered";
            case "DELETED" -> "Removed";
            case "TURNED_ON" -> "Turned on";
            case "TURNED_OFF" -> "Turned off";
            case "LOCKED" -> "Locked";
            case "UNLOCKED" -> "Unlocked";
            case "CAME_ONLINE" -> "Came online";
            case "WENT_OFFLINE" -> "Went offline";
            case "UPDATED_SETTINGS" -> "Settings updated";
            default -> action.startsWith("SET_")
                    ? "Set " + action.substring(4).toLowerCase().replace('_', ' ')
                    : action;
        };
    }

    private static Color colorFor(DeviceType type) {
        return switch (type) {
            case LIGHT -> Theme.AMBER;
            case FAN -> Theme.ACCENT;
            case AC -> new Color(120, 200, 255);
            case DOOR_LOCK -> Theme.GREEN;
            case CAMERA -> new Color(190, 130, 255);
            case SENSOR -> new Color(90, 220, 200);
        };
    }
}
