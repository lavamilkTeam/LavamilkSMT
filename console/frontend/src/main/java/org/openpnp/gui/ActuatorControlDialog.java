package org.openpnp.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openpnp.gui.support.ActuatorProfilesComboBoxModel;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ActuatorControlDialog extends JDialog {
    private JTextField valueTf;
    private JTextField readTf;
    private JLabel lblBoolean;
    private JButton onBtn;
    private JButton offBtn;
    private JLabel lblValue;
    private Actuator actuator;
    private JButton setBtn;
    private JLabel lblRead;
    private JButton readBtn;
    private JButton readWithDoubleBtn;
    private JButton closeBtn;
    private JComboBox profile;
    private JButton setProfileBtn;
    private JLabel lblSetProfile;
    private JTextField withDoubleTf;
    private JLabel lblWith;

    public ActuatorControlDialog(Actuator actuator) {
        super(MainFrame.get(), actuator.getHead() == null ? actuator.getName()
                : actuator.getHead().getName() + ":" + actuator.getName(), true);
        this.actuator = actuator;
        getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,}));

        setModalityType(JDialog.ModalityType.MODELESS);

        lblBoolean = new JLabel(org.openpnp.Translations.getString("Local.680cd6b7c76081da"));
        getContentPane().add(lblBoolean, "2, 2");

        onBtn = new JButton(org.openpnp.Translations.getString("Local.130011756125313c"));
        getContentPane().add(onBtn, "4, 2");
        onBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    AbstractActuator.assertOnOffDefined(actuator);
                    actuator.actuate(true);
                });
            }
        });

        offBtn = new JButton(org.openpnp.Translations.getString("Local.ca7981b46ecf2c17"));
        getContentPane().add(offBtn, "6, 2");
        offBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    AbstractActuator.assertOnOffDefined(actuator);
                    actuator.actuate(false);
                });
            }
        });

        lblValue = new JLabel(org.openpnp.Translations.getString("Local.05c80740881c5e9f"));
        getContentPane().add(lblValue, "2, 4, right, default");

        valueTf = new JTextField();
        getContentPane().add(valueTf, "4, 4, 3, 1");
        valueTf.setColumns(10);

        setBtn = new JButton(org.openpnp.Translations.getString("Local.b6f6f3ad07b3c05f"));
        setBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    // Use the generic Object method to interpret the value according to the Actuator.valueType.
                    actuator.actuate((Object)valueTf.getText());
                });
            }
        });
        getContentPane().add(setBtn, "8, 4");

        lblSetProfile = new JLabel(org.openpnp.Translations.getString("Local.5ecd305aa7b3330b"));
        getContentPane().add(lblSetProfile, "2, 5, right, default");

        profile = new org.openpnp.gui.components.LocalizedComboBox(new ActuatorProfilesComboBoxModel((AbstractActuator) actuator));
        getContentPane().add(profile, "4, 5, 3, 1, fill, default");

        setProfileBtn = new JButton(org.openpnp.Translations.getString("Local.b6f6f3ad07b3c05f"));
        setProfileBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(profile.getSelectedItem());
                });
            }
        });
        getContentPane().add(setProfileBtn, "8, 5");

        lblRead = new JLabel(org.openpnp.Translations.getString("Local.82902e319d89ad4e"));
        getContentPane().add(lblRead, "2, 7, right, default");

        readTf = new JTextField();
        getContentPane().add(readTf, "4, 7, 3, 1");
        readTf.setColumns(10);

        readBtn = new JButton(org.openpnp.Translations.getString("Local.9b9a8d05a7ec353b"));
        readBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    String s = actuator.read();
                    readTf.setText(s == null ? "" : s);
                });
            }
        });
        getContentPane().add(readBtn, "8, 7");

        readWithDoubleBtn = new JButton(org.openpnp.Translations.getString("Local.61044df2acc46824"));
        readWithDoubleBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    String s = actuator.read(Double.parseDouble(withDoubleTf.getText()));
                    readTf.setText(s == null ? "" : s);
                });
            }
        });

        lblWith = new JLabel(org.openpnp.Translations.getString("Local.b708724a23bf11c2"));
        getContentPane().add(lblWith, "2, 9, right, default");

        withDoubleTf = new JTextField();
        getContentPane().add(withDoubleTf, "4, 9, 3, 1, fill, default");
        withDoubleTf.setColumns(10);
        getContentPane().add(readWithDoubleBtn, "8, 9");

        closeBtn = new JButton(org.openpnp.Translations.getString("Local.7d9eb7acb13e2462"));
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        getContentPane().add(closeBtn, "8, 11");

        ((AbstractActuator)actuator).addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("valueType")) {
                    adaptDialog();            
                }
            }
        });

        adaptDialog();
    }

    private void adaptDialog() {
        boolean isBoolean = actuator.getValueType() == ActuatorValueType.Boolean;
        boolean isProfiled = actuator.getValueType() == ActuatorValueType.Profile;

        boolean showBoolean = (actuator.getDefaultOffValue() != null && actuator.getDefaultOnValue() != null
                && !actuator.getDefaultOffValue().equals(actuator.getDefaultOnValue()));
        lblBoolean.setVisible(showBoolean);
        onBtn.setVisible(showBoolean);
        offBtn.setVisible(showBoolean);

        lblValue.setVisible(!(isBoolean || isProfiled));
        valueTf.setVisible(!(isBoolean || isProfiled));
        setBtn.setVisible(!(isBoolean || isProfiled));

        lblSetProfile.setVisible(isProfiled);
        profile.setVisible(isProfiled);
        setProfileBtn.setVisible(isProfiled);
    }
}
