package com.domotiq.gui;

import com.domotiq.model.ActivityLog;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Full device control history. */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");

    private final MainFrame main;
    private final JComboBox<Integer> limitCombo =
            new JComboBox<>(new Integer[]{50, 100, 200, 500});
    private final javax.swing.JTable table = UiTables.styledTable(
            new String[]{"Date & time", "Device", "Action", "By",
                    "Previous status", "New status"}, 0);

    public HistoryPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        limitCombo.setSelectedItem(100);
        limitCombo.setFont(Theme.font(13, Font.PLAIN));

        JPanel bar = Theme.hRow(10);
        bar.add(Theme.label("Show last", 13, Font.PLAIN, Theme.TEXT_MUTED));
        bar.add(limitCombo);
        var refreshBtn = Theme.ghostButton("Refresh", Theme.ACCENT);
        bar.add(refreshBtn);

        refreshBtn.addActionListener(e -> refresh());
        limitCombo.addActionListener(e -> refresh());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(bar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void onLiveUpdate() {
    }

    public void refresh() {
        var model = UiTables.modelOf(table);
        model.setRowCount(0);
        Integer limit = (Integer) limitCombo.getSelectedItem();
        try {
            List<ActivityLog> logs =
                    main.getActivityController().recent(main.getUser().getId(), limit);
            for (ActivityLog log : logs) {
                model.addRow(new Object[]{
                        log.getPerformedAt() == null ? "" : TIME_FMT.format(log.getPerformedAt()),
                        log.getDeviceName(),
                        DashboardPanel.prettyAction(log.getAction()),
                        "AUTOMATION".equals(log.getActor()) ? "Automation" : "You",
                        dash(log.getPreviousStatus()),
                        dash(log.getNewStatus())});
            }
        } catch (SQLException ex) {
            main.showError("Could not load history: " + ex.getMessage());
        }
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
