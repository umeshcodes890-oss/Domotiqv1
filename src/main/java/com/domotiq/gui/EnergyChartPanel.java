package com.domotiq.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Hand-painted 7-day kWh bar chart (pure Swing, no chart library).
 * Re-reads its data on every paint so theme switches and refreshes are free.
 */
public class EnergyChartPanel extends JComponent {

    public record Bar(String label, double value) { }

    private List<Bar> bars = List.of();
    private String unitCaption = "kWh per day";

    public EnergyChartPanel() {
        setPreferredSize(new Dimension(760, 260));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public void setData(List<Bar> bars) {
        this.bars = bars == null ? List.of() : bars;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int left = 58, bottom = 34, top = 14, right = 12;
        int plotW = w - left - right, plotH = h - top - bottom;

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, 18, 18);

        double max = bars.stream().mapToDouble(Bar::value).max().orElse(0);
        if (max <= 0) {
            max = 1;
        }
        // round axis top up to a friendly number
        max = niceCeiling(max);

        // horizontal grid lines + axis labels
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(Theme.font(11, Font.PLAIN));
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            int y = top + plotH - plotH * i / steps;
            g2.setColor(Theme.GRID);
            g2.drawLine(left, y, w - right, y);
            g2.setColor(Theme.TEXT_MUTED);
            String lbl = String.format("%.2f", max * i / steps);
            g2.drawString(lbl, 8, y + 4);
        }

        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString(unitCaption, left, h - 6);

        if (bars.isEmpty()) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.font(13, Font.ITALIC));
            g2.drawString("No energy data yet - keep the app running with devices on.",
                    left + 10, top + plotH / 2);
            g2.dispose();
            return;
        }

        double slot = (double) plotW / bars.size();
        double barW = Math.min(slot * 0.55, 64);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            double barH = plotH * Math.min(1.0, bar.value() / max);
            int x = (int) (left + slot * i + (slot - barW) / 2);
            int y = (int) (top + plotH - barH);

            g2.setColor(bar.value() <= 0 ? Theme.GRID : Theme.ACCENT);
            g2.fillRoundRect(x, y, (int) barW, Math.max((int) barH, bar.value() > 0 ? 3 : 1),
                    8, 8);

            g2.setColor(Theme.TEXT);
            g2.setFont(Theme.font(11, Font.BOLD));
            if (bar.value() > 0) {
                String v = String.format("%.2f", bar.value());
                int tw = g2.getFontMetrics().stringWidth(v);
                g2.drawString(v, x + (int) barW / 2 - tw / 2, Math.max(y - 5, top + 10));
            }
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.font(11, Font.PLAIN));
            String dl = bar.label();
            int tw = g2.getFontMetrics().stringWidth(dl);
            g2.drawString(dl, x + (int) barW / 2 - tw / 2, top + plotH + 16);
        }
        g2.dispose();
    }

    private static double niceCeiling(double v) {
        for (double step : new double[] {0.25, 0.5, 1, 2, 5, 10, 20, 50, 100}) {
            if (v <= step) {
                return step;
            }
        }
        return Math.ceil(v / 100) * 100;
    }
}
