package com.domotiq.gui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.function.Function;

/** Tiny generic combo renderer: labels items through a function. */
public class ListCellWrap<E> extends DefaultListCellRenderer {

    private final Function<E, String> text;

    public ListCellWrap(Function<E, String> text) {
        this.text = text;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(value == null ? "" : text.apply((E) value));
        return this;
    }
}
