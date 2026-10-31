package com.domotiq.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared look & feel for every DOMOTIQ screen.
 * Colours form a swappable palette: call {@link #apply(Mode)} before building
 * any UI, then reference the static fields at component-creation time.
 */
public final class Theme {

    public enum Mode { DARK, LIGHT }

    /* ----- live palette (mutated by apply()) ----- */
    public static Color BACKGROUND;
    public static Color PANEL;
    public static Color PANEL_HOVER;
    public static Color CARD;
    public static Color ACCENT;
    public static Color ACCENT_DARK;
    public static Color GREEN;
    public static Color RED;
    public static Color AMBER;
    public static Color TEXT;
    public static Color TEXT_MUTED;
    public static Color FIELD_BG;
    public static Color BORDER;
    public static Color INPUT_BORDER;
    public static Color ROW_ALT;
    public static Color GRID;
    public static Color SIDEBAR;
    public static Color SIDEBAR_BUTTON;
    public static Color SIDEBAR_TEXT;

    public static Mode mode = Mode.DARK;

    private static final String FONT_FAMILY = "Segoe UI";

    static {
        apply(Mode.DARK);
    }

    private Theme() {
    }

    public static void apply(Mode selected) {
        mode = selected;
        if (selected == Mode.DARK) {
            BACKGROUND = new Color(15, 20, 32);
            PANEL = new Color(23, 30, 46);
            PANEL_HOVER = new Color(31, 40, 60);
            CARD = new Color(28, 36, 56);
            ACCENT = new Color(79, 140, 255);
            ACCENT_DARK = new Color(52, 100, 200);
            GREEN = new Color(52, 199, 123);
            RED = new Color(235, 90, 90);
            AMBER = new Color(240, 175, 75);
            TEXT = new Color(232, 238, 250);
            TEXT_MUTED = new Color(140, 152, 175);
            FIELD_BG = new Color(18, 24, 38);
            BORDER = new Color(58, 70, 98);
            INPUT_BORDER = new Color(60, 72, 100);
            ROW_ALT = new Color(26, 34, 52);
            GRID = new Color(40, 50, 74);
            SIDEBAR = new Color(19, 25, 40);
            SIDEBAR_BUTTON = new Color(30, 40, 62);
            SIDEBAR_TEXT = new Color(232, 238, 250);
        } else {
            BACKGROUND = new Color(244, 246, 250);
            PANEL = new Color(255, 255, 255);
            PANEL_HOVER = new Color(234, 240, 250);
            CARD = new Color(255, 255, 255);
            ACCENT = new Color(37, 99, 235);
            ACCENT_DARK = new Color(26, 72, 178);
            GREEN = new Color(22, 163, 74);
            RED = new Color(220, 38, 38);
            AMBER = new Color(217, 119, 6);
            TEXT = new Color(26, 32, 44);
            TEXT_MUTED = new Color(100, 112, 132);
            FIELD_BG = new Color(255, 255, 255);
            BORDER = new Color(210, 216, 228);
            INPUT_BORDER = new Color(203, 210, 224);
            ROW_ALT = new Color(246, 248, 252);
            GRID = new Color(226, 230, 238);
            SIDEBAR = new Color(24, 32, 50);
            SIDEBAR_BUTTON = new Color(41, 54, 82);
            SIDEBAR_TEXT = new Color(232, 238, 250);
        }
    }

    public static Font font(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(size, style));
        l.setForeground(color);
        return l;
    }

    /** Primary filled button with rounded corners and hover state. */
    public static JButton button(String text, Color base) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isArmed()
                        ? base.darker()
                        : (getModel().isRollover() ? base.brighter() : base);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(14, Font.BOLD));
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        return b;
    }

    public static JButton ghostButton(String text, Color fg) {
        JButton b = button(text, PANEL);
        b.setForeground(fg);
        b.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        return b;
    }

    public static JTextField textField(String placeholder, int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(font(14, Font.PLAIN));
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setOpaque(true);
        f.setBackground(FIELD_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return f;
    }

    /** A rounded card container. */
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    public static JPanel hRow(int gap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        return p;
    }

    /** Small coloured status dot. */
    public static JComponent statusDot(Color color) {
        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(1, 1, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        return dot;
    }

    public static void onClick(Component c, Runnable action) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
    }
}
