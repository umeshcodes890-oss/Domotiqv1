package com.domotiq.gui;

import com.domotiq.controller.EnergyController;
import com.domotiq.repository.EnergyRepository.DailyTotal;
import com.domotiq.repository.EnergyRepository.DeviceTotal;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Energy dashboard: estimates built from simulated uptime, not real meters.
 * 7-day bar chart (hand-painted) + per-device month table.
 */
public class EnergyPanel extends JPanel {

    private final MainFrame main;
    private final JLabel monthValue = Theme.label("0.00", 30, Font.BOLD, Theme.ACCENT);
    private final JLabel todayValue = Theme.label("0.00", 30, Font.BOLD, Theme.GREEN);
    private final JLabel devicesValue = Theme.label("0", 30, Font.BOLD, Theme.AMBER);
    private final EnergyChartPanel chart = new EnergyChartPanel();
    private final JTable deviceTable =
            UiTables.styledTable(new String[]{"Device", "Category", "This month (kWh)",
                    "Nominal watts", "Est. run-hours"}, 0);

    public EnergyPanel(MainFrame main) {
        this.main = main;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(6, 24, 16, 24));

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(statCard("This month (kWh)", monthValue,
                "Estimate from device uptime while DOMOTIQ was running."));
        stats.add(statCard("Today (kWh)", todayValue, "Accumulated since midnight."));
        stats.add(statCard("Metered devices", devicesValue,
                "Devices with recorded consumption this month."));

        JPanel chartCard = Theme.card();
        chartCard.setLayout(new BorderLayout(0, 6));
        chartCard.add(Theme.label("Last 7 days", 14, Font.BOLD, Theme.TEXT),
                BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.setPreferredSize(new Dimension(0, 320));

        JPanel tableCard = Theme.card();
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.add(Theme.label("Consumption by device (this month)",
                14, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(deviceTable);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
        for (JComponent p : List.of(chartCard, tableCard)) {
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
            center.add(p);
        }

        add(stats, BorderLayout.NORTH);
        add(scroll(center), BorderLayout.CENTER);

        JPanel note = Theme.hRow(6);
        note.add(Theme.label("Simulated estimates only - nameplate wattage x observed "
                + "uptime. No physical meters are involved.", 11, Font.ITALIC,
                Theme.TEXT_MUTED));
        add(note, BorderLayout.SOUTH);
    }

    private JScrollPane scroll(JPanel p) {
        JScrollPane sp = new JScrollPane(p,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private JPanel statCard(String title, JLabel value, String caption) {
        JPanel c = Theme.card();
        c.setLayout(new BorderLayout(0, 4));
        c.setPreferredSize(new Dimension(0, 118));
        c.add(Theme.label(title, 13, Font.BOLD, Theme.TEXT_MUTED), BorderLayout.NORTH);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel mid = Theme.hRow(8);
        mid.add(value);
        c.add(mid, BorderLayout.CENTER);
        c.add(Theme.label(caption, 10, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.SOUTH);
        return c;
    }

    public void refresh() {
        EnergyController energy = main.getEnergyController();
        int userId = main.getUser().getId();
        try {
            double month = energy.monthTotal(userId);
            double today = energy.todayTotal(userId);
            DeviceTotal[] byDevice = energy.monthByDevice(userId);
            List<DailyTotal> week = energy.lastDays(userId, 7);

            monthValue.setText(String.format("%.2f", month));
            todayValue.setText(String.format("%.2f", today));
            devicesValue.setText(String.valueOf(byDevice.length));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE");
            List<EnergyChartPanel.Bar> bars = new ArrayList<>();
            for (DailyTotal d : week) {
                bars.add(new EnergyChartPanel.Bar(d.date().format(fmt), d.kwh()));
            }
            chart.setData(bars);

            var model = (javax.swing.table.DefaultTableModel) deviceTable.getModel();
            model.setRowCount(0);
            for (DeviceTotal dt : byDevice) {
                double watts = deviceWatts(dt.deviceId());
                double hours = watts > 0 ? dt.kwh() * 1000.0 / watts : 0;
                model.addRow(new Object[] {
                        dt.deviceName(),
                        prettyType(dt.deviceType()),
                        String.format("%.3f", dt.kwh()),
                        watts + " W",
                        String.format("%.1f h", hours)});
            }
        } catch (Exception ex) {
            monthValue.setText("—");
            todayValue.setText("—");
            devicesValue.setText("—");
            chart.setData(List.of());
        }
    }

    private double deviceWatts(int deviceId) {
        return main.getDeviceController().getDevices().stream()
                .filter(d -> d.getId() == deviceId)
                .map(d -> (double) d.nominalWatts())
                .findFirst().orElse(0.0);
    }

    private static String prettyType(String enumVal) {
        try {
            return com.domotiq.model.DeviceType.valueOf(enumVal).getLabel();
        } catch (IllegalArgumentException ex) {
            return enumVal;
        }
    }
}
