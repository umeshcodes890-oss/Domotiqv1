package com.domotiq.gui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One navigation rail entry with idle, hover, pressed, selected and keyboard-focus
 * states. State changes tween over Theme.MOTION_MS instead of snapping, and the
 * selected item is marked by an accent bar plus label weight, never by colour alone.
 */
class NavButton extends JToggleButton {

    private static final float STEP = 30f / Theme.MOTION_MS;

    private final String label;
    private final NavIcons.Kind kind;
    private final javax.swing.Timer ticker;

    private float hover;
    private float hoverTarget;
    private float pressed;
    private float pressedTarget;
    private boolean collapsed;

    NavButton(String label, NavIcons.Kind kind, String shortcut) {
        super(label);
        this.label = label;
        this.kind = kind;

        setFont(Theme.font(15, Font.PLAIN));
        setForeground(Theme.SIDEBAR_TEXT);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setHorizontalAlignment(LEADING);
        setIconTextGap(12);
        setIcon(new Glyph());
        applyInsets();

        setToolTipText(label + "  (" + shortcut + ")");
        getAccessibleContext().setAccessibleName(label + ", " + shortcut + " key");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        getModel().addChangeListener(e -> {
            setFont(Theme.font(15, isSelected() ? Font.BOLD : Font.PLAIN));
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hoverTarget = 1f;
                tick();
            }

            @Override public void mouseExited(MouseEvent e) {
                hoverTarget = 0f;
                pressedTarget = 0f;
                tick();
            }

            @Override public void mousePressed(MouseEvent e) {
                pressedTarget = 1f;
                tick();
            }

            @Override public void mouseReleased(MouseEvent e) {
                pressedTarget = 0f;
                tick();
            }
        });

        ticker = new javax.swing.Timer(30, e -> tick());
    }

    /** Advances the state floats one frame; keeps the timer alive until settled. */
    private void tick() {
        boolean busy = false;
        if (hover != hoverTarget) {
            hover = approach(hover, hoverTarget);
            busy = true;
        }
        if (pressed != pressedTarget) {
            pressed = approach(pressed, pressedTarget);
            busy = true;
        }
        repaint();
        if (!busy) {
            ticker.stop();
        } else if (!ticker.isRunning()) {
            ticker.start();
        }
    }

    private static float approach(float value, float target) {
        float delta = Math.min(STEP, Math.abs(target - value));
        return value < target ? value + delta : value - delta;
    }

    void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        setText(collapsed ? "" : label);
        setHorizontalAlignment(collapsed ? CENTER : LEADING);
        applyInsets();
        revalidate();
        repaint();
    }

    private void applyInsets() {
        setBorder(BorderFactory.createEmptyBorder(0, collapsed ? 0 : 14, 0,
                collapsed ? 0 : 8));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Color to = isSelected() ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR_HOVER;
        float amount = isSelected() ? Math.max(0.7f, hover) : hover;
        Color fill = Theme.blend(Theme.SIDEBAR, to, amount);
        if (pressed > 0.01f) {
            fill = Theme.blend(fill, fill.darker(), pressed);
        }
        if (amount > 0.01f || pressed > 0.01f) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 3, getWidth() - 1, getHeight() - 7, 12, 12);
        }
        if (isSelected()) {
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(0, 12, 4, getHeight() - 25, 4, 4);
        }
        if (hasFocus()) {
            g2.setColor(Theme.SIDEBAR_TEXT);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 4, getWidth() - 3, getHeight() - 9, 12, 12);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Vector glyph tinted by the button's state, painted by Swing's icon slot. */
    private class Glyph implements Icon {

        @Override public int getIconWidth() {
            return 24;
        }

        @Override public int getIconHeight() {
            return 24;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color muted = Theme.blend(Theme.SIDEBAR_TEXT, Theme.SIDEBAR_NOTE, 0.35f);
            Color text = Theme.blend(isSelected() ? Theme.SIDEBAR_TEXT : muted,
                    Theme.SIDEBAR_TEXT, hover);
            NavIcons.paint((Graphics2D) g, kind, x + 1, y, 22, text);
        }
    }
}
