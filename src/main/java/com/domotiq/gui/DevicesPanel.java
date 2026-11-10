package com.domotiq.gui;

import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

/** All devices: search, filter by category and room, add, manage. */
public class DevicesPanel extends JPanel {

    private final MainFrame main;
    private final JTextField searchField = Theme.textField("Search devices...", 22);
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> roomFilter = new JComboBox<>();
    private final JPanel grid = new GradientSurface(new GridLayout(0, 3, 16, 16));

    public DevicesPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 20, 24));

        typeFilter.addItem("All categories");
        for (DeviceType t : DeviceType.values()) {
            typeFilter.addItem(t.getLabel());
        }
        roomFilter.addItem("All rooms");
        roomFilter.addItem("Unassigned");
        style(typeFilter);
        style(roomFilter);

        JPanel toolbar = Theme.hRow(10);
        toolbar.add(searchField);
        toolbar.add(typeFilter);
        toolbar.add(roomFilter);
        var addBtn = Theme.button("+ Add device", Theme.GREEN);
        JPanel right = Theme.hRow(10);
        right.add(addBtn);
        addBtn.addActionListener(e ->
                new DeviceDialog(main, null, true).setVisible(true));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(toolbar, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        grid.setOpaque(false);
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(new java.awt.Color(0, 0, 0, 0));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // adaptive columns: cards always fit the viewport, never clip
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int cols = Math.max(1, e.getComponent().getWidth() / 316);
                GridLayout gl = (GridLayout) grid.getLayout();
                if (gl.getColumns() != cols) {
                    gl.setColumns(cols);
                    grid.revalidate();
                }
            }
        });

        add(bar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(
                new SimpleDocListener(this::refresh));
        typeFilter.addActionListener(e -> refresh());
        roomFilter.addActionListener(e -> refresh());
    }

    private static void style(JComboBox<String> c) {
        c.setFont(Theme.font(13, Font.PLAIN));
        c.setForeground(Theme.TEXT);
        c.setBackground(Theme.FIELD_BG);
    }

    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;

        SimpleDocListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }

    public void onLiveUpdate() {
        refresh();
    }

    public void refresh() {
        List<Room> rooms;
        try {
            rooms = main.getRoomController().listRooms(main.getUser().getId());
        } catch (SQLException ex) {
            rooms = List.of();
        }

        // keep room filter in sync
        String selectedRoom = (String) roomFilter.getSelectedItem();
        roomFilter.removeAllItems();
        roomFilter.addItem("All rooms");
        roomFilter.addItem("Unassigned");
        rooms.forEach(r -> roomFilter.addItem(r.getName()));
        roomFilter.setSelectedItem(selectedRoom);

        DeviceType typeFilterValue = matchType((String) typeFilter.getSelectedItem());
        String selectedRoomLabel = (String) roomFilter.getSelectedItem();
        boolean unassignedOnly = "Unassigned".equals(selectedRoomLabel);
        Integer roomIdValue = matchRoom(selectedRoomLabel, rooms);

        List<Device> visible = main.getDeviceController()
                .search(searchField.getText(), typeFilterValue, roomIdValue);
        if (unassignedOnly) {
            visible = visible.stream().filter(d -> d.getRoomId() == null).toList();
        }

        grid.removeAll();
        if (visible.isEmpty()) {
            JPanel empty = Theme.card();
            empty.setMinimumSize(new Dimension(300, 120));
            empty.setPreferredSize(new Dimension(300, 120));
            empty.add(Theme.label(
                    main.getDeviceController().countAll() == 0
                            ? "No devices yet. Click \"+ Add device\" to register your first simulated IoT device."
                            : "No devices match the current search or filters.",
                    14, Font.PLAIN, Theme.TEXT_MUTED));
            grid.add(Theme.gridCell(empty));
            grid.revalidate();
            grid.repaint();
            return;
        }
        for (Device device : visible) {
            grid.add(Theme.gridCell(new DeviceCard(main, device, rooms, true)));
        }
        grid.revalidate();
        grid.repaint();
    }

    private DeviceType matchType(String label) {
        if (label == null || label.equals("All categories")) {
            return null;
        }
        for (DeviceType t : DeviceType.values()) {
            if (t.getLabel().equals(label)) {
                return t;
            }
        }
        return null;
    }

    private Integer matchRoom(String label, List<Room> rooms) {
        if (label == null || label.equals("All rooms")) {
            return null;
        }
        if (label.equals("Unassigned")) {
            return null; // handled by the unassignedOnly flag
        }
        return rooms.stream().filter(r -> r.getName().equals(label))
                .map(Room::getId).findFirst().orElse(null);
    }
}
