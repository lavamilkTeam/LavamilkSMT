package org.openpnp.vision.pipeline.ui;

import org.openpnp.spi.Actuator;

import com.l2fprod.common.beans.editor.BooleanAsCheckBoxPropertyEditor;
import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.beans.editor.DoublePropertyEditor;
import com.l2fprod.common.beans.editor.StringPropertyEditor;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.renderer.BooleanCellRenderer;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

public class PipelinePropertySheetTable extends PropertySheetTable {
    private PipelinePanel pipelinePanel;

    public PipelinePropertySheetTable(PipelinePanel pipelinePanel) {
        this.pipelinePanel = pipelinePanel;
    }

    /** Supply translated enum labels without replacing the enum values used by setters/XML. */
    public void localizeEnumProperties() {
        for (com.l2fprod.common.propertysheet.Property property : getSheetModel().getProperties()) {
            if (property.getType().isEnum()) {
                ComboBoxPropertyEditor editor = new ComboBoxPropertyEditor();
                editor.setAvailableValues(property.getType().getEnumConstants());
                javax.swing.JComboBox<?> combo = (javax.swing.JComboBox<?>) editor.getCustomEditor();
                combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                            Object value, int index, boolean selected, boolean focus) {
                        super.getListCellRendererComponent(list, value, index, selected, focus);
                        setText(org.openpnp.Translations.display(value));
                        return this;
                    }
                });
                getEditorRegistry().registerEditor(property, editor);
                getRendererRegistry().registerRenderer(property.getType(), new javax.swing.table.DefaultTableCellRenderer() {
                    @Override protected void setValue(Object value) {
                        setText(org.openpnp.Translations.display(value));
                    }
                });
            }
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        super.setValueAt(aValue, row, column);

        pipelinePanel.onStagePropertySheetValueChanged(aValue, row, column);
    }

    /**
     * Provide support for Actuator variant value types. 
     * 
     * @param table
     * @param propertyName
     * @param actuator
     */
    public void customizeActuatorProperty(String propertyName,
            Actuator actuator) {
        if (actuator != null) {
            PropertySheetTableModel model = (PropertySheetTableModel)getModel();
            com.l2fprod.common.propertysheet.Property actuatorValueProperty = null; 
            for (com.l2fprod.common.propertysheet.Property property : model.getProperties()) {
                if (property.getName().equals(propertyName)) {
                    actuatorValueProperty = property;
                    break;
                }
            }
            if (actuatorValueProperty != null) {
                switch (actuator.getValueType()) {
                    case Boolean:
                        getEditorRegistry().registerEditor(actuatorValueProperty, new BooleanAsCheckBoxPropertyEditor());
                        getRendererRegistry().registerRenderer(Object.class, new BooleanCellRenderer());
                        getRendererRegistry().registerRenderer(String.class, new DefaultCellRenderer());
                        break;
                    case Double:
                        getEditorRegistry().registerEditor(actuatorValueProperty, new DoublePropertyEditor());
                        getRendererRegistry().registerRenderer(Object.class, new DefaultCellRenderer());
                        break;
                    case String:
                        getEditorRegistry().registerEditor(actuatorValueProperty, new StringPropertyEditor());
                        getRendererRegistry().registerRenderer(Object.class, new DefaultCellRenderer());
                        break;
                    case Profile:
                        getEditorRegistry().registerEditor(actuatorValueProperty, new ActuatorProfileEditor(actuator.getProfileValues()));
                        getRendererRegistry().registerRenderer(Object.class, new DefaultCellRenderer());
                        break;
                }
            }
        }
    }

    public static class ActuatorProfileEditor extends ComboBoxPropertyEditor {
        public ActuatorProfileEditor(String [] values) {
            super();
            setAvailableValues(values);
        }
    }
}
