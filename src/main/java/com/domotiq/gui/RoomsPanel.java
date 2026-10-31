package com.domotiq.gui;

import com.domotiq.model.Device;
import com.domotiq.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Room management with a per-room device overview. */
public class RoomsPanel extends JPanel {

    private static final DateTimeFormatter ADDED_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MainFrame main;
    private final JPanel roomList = new JPanel();
    private final JTable deviceTable = UiTables.styledTable(
            new String[]{"Device", "Category", "Status", "Connection", "Added"}, 0);
    private final java.util.LinkedHashMap<JPanel, Room> cardToRoom =
            new java.util.LinkedHashMap<>();
    private JPanel selectedCard;

    public RoomsPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(18, 14));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        roomList.setOpaque(false);
        roomList.setLayout(new javax.swing.BoxLayout(roomList, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane listScroll = new JScrollPane(roomList);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setHorizontalScrollBarPolicy(
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setPreferredSize(new Dimension(360, 0));

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.add(Theme.label("Rooms", 15, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);

        JPanel rightPanel = Theme.card();
        rightPanel.setLayout(new BorderLayout(0, 10));
        rightPanel.add(Theme.label("Devices in selected room", 15, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(deviceTable);
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setBackground(Theme.PANEL);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        deviceTable.getSelectionModel().addListSelectionListener(e -> { });

        var addBtn = Theme.button("+ Add room", Theme.GREEN);
        addBtn.addActionListener(e -> new RoomDialog(main, null).setVisible(true));

        JPanel bar = Theme.hRow(10);
        bar.add(addBtn);

        JSplitPaneHack.split(this, left, rightPanel, bar);
    }

    /** Split pane wrapper kept private to this screen. */
    private static final class JSplitPaneHack {
        static void split(RoomsPanel panel, JPanel left, JPanel right, JPanel bar) {
            var split = new javax.swing.JSplitPane(
                    javax.swing.JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setDividerLocation(380);
            split.setOpaque(false);
            split.setBorder(BorderFactory.createEmptyBorder());
            split.setBackground(Theme.BACKGROUND);
            split.getLeftComponent().setMinimumSize(new Dimension(280, 0));
            panel.add(bar, BorderLayout.NORTH);
            panel.add(split, BorderLayout.CENTER);
        }
    }

    public void onLiveUpdate() {
    }

    public void refresh() {
        cardToRoom.clear();
        roomList.removeAll();
        UiTables.modelOf(deviceTable).setRowCount(0);
        selectedCard = null;

        List<Room> rooms;
        try {
            rooms = main.getRoomController().listRooms(main.getUser().getId());
        } catch (SQLException ex) {
            main.showError("Could not load rooms: " + ex.getMessage());
            return;
        }

        if (rooms.isEmpty()) {
            JPanel empty = Theme.card();
            empty.add(Theme.label("No rooms yet. Create one to organise your devices.",
                    13, Font.PLAIN, Theme.TEXT_MUTED));
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            roomList.add(empty);
        }

        for (Room room : rooms) {
            roomList.add(roomCard(room));
        }
        roomList.revalidate();
        roomList.repaint();
    }

    private JPanel roomCard(Room room) {
        long deviceCount = main.getDeviceController().getDevices().stream()
                .filter(d -> d.getRoomId() != null && d.getRoomId() == room.getId())
                .count();

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(Theme.label(room.getName(), 15, Font.BOLD, Theme.TEXT),
                BorderLayout.WEST);
        titleRow.add(Theme.label(deviceCount + " device" + (deviceCount == 1 ? "" : "s"),
                12, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.EAST);

        String desc = room.getDescription() == null || room.getDescription().isBlank()
                ? "No description" : room.getDescription();
        JLabel descLabel = Theme.label(desc, 12, Font.PLAIN, Theme.TEXT_MUTED);

        JPanel actions = Theme.hRow(8);
        var edit = Theme.ghostButton("Edit", Theme.ACCENT);
        edit.setFont(Theme.font(11, Font.BOLD));
        edit.addActionListener(e -> new RoomDialog(main, room).setVisible(true));
        var delete = Theme.ghostButton("Delete", Theme.RED);
        delete.setFont(Theme.font(11, Font.BOLD));
        delete.addActionListener(e -> deleteRoom(room));
        actions.add(edit);
        actions.add(delete);

        card.add(titleRow, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(descLabel, BorderLayout.NORTH);
        center.add(actions, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);

        card.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        Theme.onClick(card, () -> showRoomDevices(room, card));

        cardToRoom.put(card, room);
        return card;
    }

    private void showRoomDevices(Room room, JPanel card) {
        if (selectedCard != null) {
            selectedCard.setBackground(Theme.CARD);
        }
        selectedCard = card;
        card.setBackground(Theme.PANEL_HOVER);

        var model = UiTables.modelOf(deviceTable);
        model.setRowCount(0);
        for (Device device : main.getDeviceController().getDevices()) {
            if (device.getRoomId() != null && device.getRoomId() == room.getId()) {
                model.addRow(new Object[]{
                        device.getName(),
                        device.getDeviceType().getLabel(),
                        device.statusLine(),
                        device.isOnline() ? "ONLINE" : "OFFLINE",
                        device.getDateAdded() == null
                                ? "—" : ADDED_FMT.format(device.getDateAdded())});
            }
        }
    }

    private void deleteRoom(Room room) {
        long deviceCount = main.getDeviceController().getDevices().stream()
                .filter(d -> d.getRoomId() != null && d.getRoomId() == room.getId())
                .count();
        String warning = deviceCount == 0
                ? "Delete room \"" + room.getName() + "\"?"
                : "Delete room \"" + room.getName() + "\"?\n"
                        + deviceCount + " device(s) in it will become unassigned "
                        + "(they are NOT deleted).";
        int choice = JOptionPane.showConfirmDialog(this, warning, "Delete room",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                main.getRoomController().deleteRoom(room);
                main.onDataChanged();
            } catch (SQLException ex) {
                main.showError(ex.getMessage());
            }
        }
    }
}
