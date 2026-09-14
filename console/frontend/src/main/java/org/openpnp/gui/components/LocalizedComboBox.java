package org.openpnp.gui.components;

import java.awt.Component;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import org.openpnp.Translations;

/** Localizes enum labels while retaining the original model values and custom renderer styling. */
@SuppressWarnings("serial")
public class LocalizedComboBox<E> extends JComboBox<E> {
    public LocalizedComboBox() { super(); }
    public LocalizedComboBox(E[] items) { super(items); }
    public LocalizedComboBox(Vector<E> items) { super(items); }
    public LocalizedComboBox(ComboBoxModel<E> model) { super(model); }

    @Override
    public void setRenderer(ListCellRenderer<? super E> renderer) {
        final ListCellRenderer<? super E> delegate = renderer == null ? new DefaultListCellRenderer() : renderer;
        super.setRenderer((list, value, index, selected, focus) -> {
            Component component = delegate.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Enum<?> && component instanceof JLabel) {
                ((JLabel) component).setText(Translations.display(value));
            }
            return component;
        });
    }
}
