package com.domotiq.gui;

import com.domotiq.controller.DeviceController;
import com.domotiq.model.Device;
import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Reusable rounded card for one device, used on the dashboard and device grid. */
public class DeviceCard extends JPanel {

    private static final Map<String, String> ICONS = Map.of(
            "LIGHT", "\u2600", "FAN", "\u2744", "AC", "\u26C4",
            "DOOR_LOCK", "\uD83D\uDD10", "CAMERA", "\uD83D\uDCF7", "SENSOR", "\u25C9");

    private final MainFrame main;
    private final Device device;
    private final boolean showManage;
    private javax.swing.JButton toggleButton;

    public DeviceCard(MainFrame main, Device device, List<Room> rooms, boolean showManage) {
        this.main = main;
        this.device = device;
        this.showManage = showManage;
        setLayout(new BorderLayout(10, 6));
        Theme.card().setOpaque(false);
        setOpaque(false);
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(16),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        setPreferredSize(new Dimension(348, 158));
        rebuild(rooms);
    }

    private void rebuild(List<Room> rooms) {
        removeAll();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel left = Theme.hRow(8);
        left.add(Theme.statusDot(device.isOnline()
                ? (device.isOn() ? Theme.GREEN : Theme.ACCENT) : Theme.RED));
        left.add(Theme.label(icon() + "  " + device.getName(), 16, Font.BOLD, Theme.TEXT));
        top.add(left, BorderLayout.WEST);

        String roomName = device.getRoomId() == null ? "Unassigned"
                : rooms.stream().filter(r -> r.getId() == device.getRoomId())
                        .map(Room::getName).findFirst().orElse("Unassigned");
        top.add(Theme.label(device.getDeviceType().getLabel() + " · " + roomName,
                12, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel status = Theme.label(device.statusLine(), 13, Font.PLAIN,
                device.isOnline() ? Theme.TEXT : Theme.AMBER);
        bottom.add(status, BorderLayout.WEST);

        DeviceController controller = main.getDeviceController();
        boolean lock = device.getDeviceType() == com.domotiq.model.DeviceType.DOOR_LOCK;
        String onText = lock ? "Lock" : "On";
        String offText = lock ? "Unlock" : "Off";
        toggleButton = Theme.button(device.isOn() ? offText : onText,
                device.isOnline() ? (device.isOn() ? Theme.RED : Theme.GREEN) : Color.GRAY);
        toggleButton.setFont(Theme.font(14, Font.BOLD));
        toggleButton.setEnabled(device.isOnline());
        toggleButton.addActionListener(e -> {
            try {
                controller.togglePower(device);
                main.onDataChanged();
            } catch (SQLException | IllegalStateException ex) {
                main.showError(ex.getMessage());
            }
        });

        JPanel actions = Theme.hRow(8);
        actions.add(toggleButton);
        if (showManage) {
            var manage = Theme.ghostButton("Manage", Theme.ACCENT);
            manage.setFont(Theme.font(14, Font.BOLD));
            manage.addActionListener(e -> new DeviceDialog(main, device, true).setVisible(true));
            actions.add(manage);
        }
        bottom.add(actions, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(Theme.label(simulatedHint(), 11, Font.ITALIC, Theme.TEXT_MUTED), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private String icon() {
        return ICONS.getOrDefault(device.getDeviceType().name(), "\u25A4");
    }

    private String simulatedHint() {
        return "simulated device · " + (device.isOnline() ? "connected" : "offline");
    }

    /** Rounded border for cards and inputs. */
    static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(java.awt.Component c, java.awt.Graphics g,
                                int x, int y, int width, int height) {
            var g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(2, 2, 2, 2);
        }
    }
}
