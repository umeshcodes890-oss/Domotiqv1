package com.domotiq.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Hand-drawn vector glyphs for the navigation rail. Everything is stroked from a
 * single 24-unit grid with one relative stroke weight, so icons stay crisp at any
 * size and follow the palette instead of being fixed-bitmap or emoji assets.
 */
final class NavIcons {

    enum Kind {
        DASHBOARD, DEVICES, ROOMS, AUTOMATIONS, ENERGY, HISTORY,
        CHEVRON_LEFT, CHEVRON_RIGHT, SUN, MOON
    }

    private NavIcons() {
    }

    /** Draws a glyph inside the box whose top-left corner is (x, y). */
    static void paint(Graphics2D g, Kind kind, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        float scale = size / 24f;
        g2.scale(scale, scale);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (kind) {
            case DASHBOARD -> {
                g2.drawRoundRect(3, 3, 8, 10, 3, 3);
                g2.drawRoundRect(13, 3, 8, 6, 3, 3);
                g2.drawRoundRect(13, 11, 8, 10, 3, 3);
                g2.drawRoundRect(3, 15, 8, 6, 3, 3);
            }
            case DEVICES -> {
                g2.drawOval(6, 3, 12, 12);
                g2.drawLine(9, 17, 15, 17);
                g2.drawLine(10, 20, 14, 20);
            }
            case ROOMS -> {
                g2.drawRoundRect(5, 3, 14, 18, 3, 3);
                g2.fillOval(15, 11, 2, 2);
            }
            case AUTOMATIONS -> {
                g2.drawOval(3, 2, 6, 6);
                g2.drawOval(15, 2, 6, 6);
                g2.drawOval(9, 15, 6, 6);
                g2.drawLine(6, 8, 11, 15);
                g2.drawLine(18, 8, 13, 15);
            }
            case ENERGY -> {
                Path2D.Double bolt = new Path2D.Double();
                bolt.moveTo(13, 2);
                bolt.lineTo(6, 13);
                bolt.lineTo(11, 13);
                bolt.lineTo(9, 22);
                bolt.lineTo(18, 10);
                bolt.lineTo(12.5, 10);
                bolt.closePath();
                g2.draw(bolt);
            }
            case HISTORY -> {
                g2.drawOval(3, 3, 18, 18);
                g2.drawLine(12, 7, 12, 12);
                g2.drawLine(12, 12, 16, 14);
            }
            case CHEVRON_LEFT -> {
                g2.drawLine(14, 5, 8, 12);
                g2.drawLine(8, 12, 14, 19);
            }
            case CHEVRON_RIGHT -> {
                g2.drawLine(10, 5, 16, 12);
                g2.drawLine(16, 12, 10, 19);
            }
            case SUN -> {
                g2.drawOval(8, 8, 8, 8);
                g2.drawLine(12, 2, 12, 5);
                g2.drawLine(12, 19, 12, 22);
                g2.drawLine(2, 12, 5, 12);
                g2.drawLine(19, 12, 22, 12);
                g2.drawLine(5, 5, 7, 7);
                g2.drawLine(17, 17, 19, 19);
                g2.drawLine(17, 7, 19, 5);
                g2.drawLine(5, 17, 7, 19);
            }
            case MOON -> {
                var crescent = new java.awt.geom.Area(
                        new java.awt.geom.Ellipse2D.Double(5, 3, 15, 18));
                crescent.subtract(new java.awt.geom.Area(
                        new java.awt.geom.Ellipse2D.Double(11, 1, 16, 16)));
                g2.draw(crescent);
            }
        }
        g2.dispose();
    }
}
