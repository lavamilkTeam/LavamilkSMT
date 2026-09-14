/*
 * Copyright (C) 2019 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */


package org.openpnp.machine.reference.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ContactProbeNozzle;
import org.openpnp.machine.reference.ContactProbeNozzle.ContactProbeMethod;
import org.openpnp.machine.reference.ContactProbeNozzle.ContactProbeTrigger;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ContactProbeNozzleWizard extends AbstractConfigurationWizard {
    private final ContactProbeNozzle nozzle;

    public ContactProbeNozzleWizard(ContactProbeNozzle nozzle) {
        this.nozzle = nozzle;
        createUi();
    }
    private void createUi() {

        contentPanel.setLayout(new BorderLayout(0, 0));

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.0125dff4deca8970"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblMethod = new JLabel(org.openpnp.Translations.getString("Local.52a0f9b65b278850"));
        panel.add(lblMethod, "2, 2, right, default");

        contactProbeMethod = new org.openpnp.gui.components.LocalizedComboBox(ContactProbeMethod.values());
        contactProbeMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panel.add(contactProbeMethod, "4, 2, fill, default");

        lblContactProbeActuator = new JLabel(org.openpnp.Translations.getString("Local.68c86d644d65a459"));
        panel.add(lblContactProbeActuator, "2, 4, right, center");

        comboBoxContactProbeActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxContactProbeActuator.setMaximumRowCount(15);
        comboBoxContactProbeActuator.setModel(new ActuatorsComboBoxModel(nozzle.getHead()));
        panel.add(comboBoxContactProbeActuator, "4, 4, default, top");
        
        lblProbeSpeed = new JLabel(org.openpnp.Translations.getString("Local.53941491c2e4e102"));
        lblProbeSpeed.setToolTipText(org.openpnp.Translations.getString("Local.3afb09732223a385"));
        panel.add(lblProbeSpeed, "2, 6, right, default");
        
        contactProbeSpeed = new JTextField();
        panel.add(contactProbeSpeed, "4, 6, fill, default");
        contactProbeSpeed.setColumns(10);

        lblStartOffset = new JLabel(org.openpnp.Translations.getString("Local.2b063a4be1f0cc38"));
        lblStartOffset.setToolTipText(org.openpnp.Translations.getString("Local.258dc094cfe6c461"));
        panel.add(lblStartOffset, "2, 8, right, default");

        contactProbeStartOffsetZ = new JTextField();
        panel.add(contactProbeStartOffsetZ, "4, 8, fill, default");
        contactProbeStartOffsetZ.setColumns(10);

        lblProbeDepth = new JLabel(org.openpnp.Translations.getString("Local.50c6f3cf86a314c6"));
        lblProbeDepth.setToolTipText(org.openpnp.Translations.getString("Local.7ce69771689653f8"));
        panel.add(lblProbeDepth, "2, 10, right, default");

        contactProbeDepthZ = new JTextField();
        panel.add(contactProbeDepthZ, "4, 10, fill, default");
        contactProbeDepthZ.setColumns(10);

        lblSniffleIncrement = new JLabel(org.openpnp.Translations.getString("Local.cb5728977eec029d"));
        lblSniffleIncrement.setToolTipText(org.openpnp.Translations.getString("Local.938d1daf06e6e94c"));
        panel.add(lblSniffleIncrement, "2, 12, right, default");

        sniffleIncrementZ = new JTextField();
        panel.add(sniffleIncrementZ, "4, 12, fill, default");
        sniffleIncrementZ.setColumns(10);
        
        lblSniffleDwellTime = new JLabel(org.openpnp.Translations.getString("Local.06d551aeeabccc35"));
        panel.add(lblSniffleDwellTime, "2, 14, right, default");
        
        sniffleDwellTime = new JTextField();
        panel.add(sniffleDwellTime, "4, 14, fill, default");
        sniffleDwellTime.setColumns(10);

        lblFinalAdjustment = new JLabel(org.openpnp.Translations.getString("Local.d09c1f69c321b61a"));
        lblFinalAdjustment.setToolTipText(org.openpnp.Translations.getString("Local.62490f42e0a1ae81"));
        panel.add(lblFinalAdjustment, "2, 16, right, default");

        contactProbeAdjustZ = new JTextField();
        panel.add(contactProbeAdjustZ, "4, 16, fill, default");
        contactProbeAdjustZ.setColumns(10);

        lblFeederHeightProbing = new JLabel(org.openpnp.Translations.getString("Local.001cd1b0d27a3fb1"));
        lblFeederHeightProbing.setToolTipText(org.openpnp.Translations.getString("Local.a4d4c5296211104d"));
        panel.add(lblFeederHeightProbing, "2, 20, right, default");

        feederHeightProbing = new org.openpnp.gui.components.LocalizedComboBox(ContactProbeTrigger.values());
        panel.add(feederHeightProbing, "4, 20, fill, default");

        lblPartHeightProbing = new JLabel(org.openpnp.Translations.getString("Local.e481f74a684b25fc"));
        lblPartHeightProbing.setToolTipText(org.openpnp.Translations.getString("Local.0d67023d9b12b8be"));
        panel.add(lblPartHeightProbing, "2, 22, right, default");

        partHeightProbing = new org.openpnp.gui.components.LocalizedComboBox(ContactProbeTrigger.values());
        panel.add(partHeightProbing, "4, 22, fill, default");
        
        lblDiscardProbing = new JLabel(org.openpnp.Translations.getString("Local.664f12fa03eb96b2"));
        lblDiscardProbing.setToolTipText(org.openpnp.Translations.getString("Local.d8fd1b42df5646ef"));
        panel.add(lblDiscardProbing, "2, 24, right, default");

        discardProbing = new JCheckBox("");
        panel.add(discardProbing, "4, 24");

        lblZCalibration = new JLabel(org.openpnp.Translations.getString("Local.d9e8589b3e550d2c"));
        panel.add(lblZCalibration, "2, 28, right, default");
        
        calibrationOffsetZ = new JTextField();
        calibrationOffsetZ.setEditable(false);
        panel.add(calibrationOffsetZ, "4, 28, fill, default");
        calibrationOffsetZ.setColumns(10);
        
        btnCalibrateNow = new JButton(calibrateZAction);
        panel.add(btnCalibrateNow, "6, 28");
    }

    protected void adaptDialog() {
        ContactProbeMethod method = (ContactProbeMethod) contactProbeMethod.getSelectedItem();
        boolean isProbing = (method != ContactProbeMethod.None);
        boolean isActuator = (method == ContactProbeMethod.ContactSenseActuator);
        boolean isVacuum = (method == ContactProbeMethod.VacuumSense);

        lblContactProbeActuator.setVisible(isActuator);
        comboBoxContactProbeActuator.setVisible(isActuator);
        lblStartOffset.setVisible(isProbing);
        contactProbeStartOffsetZ.setVisible(isProbing);
        lblSniffleIncrement.setVisible(isVacuum);
        sniffleIncrementZ.setVisible(isVacuum);
        lblSniffleDwellTime.setVisible(isVacuum);
        sniffleDwellTime.setVisible(isVacuum);
        lblProbeDepth.setVisible(isProbing);
        contactProbeDepthZ.setVisible(isProbing);
        lblFinalAdjustment.setVisible(isProbing);
        contactProbeAdjustZ.setVisible(isProbing);
        lblProbeSpeed.setVisible(isProbing);
        contactProbeSpeed.setVisible(isProbing);

        lblFeederHeightProbing.setVisible(isActuator);
        feederHeightProbing.setVisible(isActuator);
        lblPartHeightProbing.setVisible(isActuator);
        partHeightProbing.setVisible(isActuator);
        lblDiscardProbing.setVisible(isActuator);
        discardProbing.setVisible(isActuator);

        lblZCalibration.setVisible(isProbing);
        calibrationOffsetZ.setVisible(isProbing);
        btnCalibrateNow.setVisible(isProbing);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Actuator> actuatorConverter = (new NamedConverter<>(nozzle.getHead().getActuators()));

        addWrappedBinding(nozzle, "contactProbeMethod", contactProbeMethod, "selectedItem");
        addWrappedBinding(nozzle, "contactProbeActuator", comboBoxContactProbeActuator, "selectedItem", actuatorConverter);

        addWrappedBinding(nozzle, "contactProbeStartOffsetZ", contactProbeStartOffsetZ, "text", lengthConverter);
        addWrappedBinding(nozzle, "contactProbeDepthZ", contactProbeDepthZ, "text", lengthConverter);
        addWrappedBinding(nozzle, "contactProbeSpeed", contactProbeSpeed, "text", doubleConverter);
        addWrappedBinding(nozzle, "sniffleIncrementZ", sniffleIncrementZ, "text", lengthConverter);
        addWrappedBinding(nozzle, "sniffleDwellTime", sniffleDwellTime, "text", longConverter);
        addWrappedBinding(nozzle, "contactProbeAdjustZ", contactProbeAdjustZ, "text", lengthConverter);

        addWrappedBinding(nozzle, "feederHeightProbing", feederHeightProbing, "selectedItem");
        addWrappedBinding(nozzle, "partHeightProbing", partHeightProbing, "selectedItem");
        addWrappedBinding(nozzle, "discardProbing", discardProbing, "selected");

        addWrappedBinding(nozzle, "calibrationOffsetZ", calibrationOffsetZ, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(contactProbeStartOffsetZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(contactProbeDepthZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(sniffleIncrementZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(contactProbeAdjustZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationOffsetZ);
        
        adaptDialog();
    }

    private Action calibrateZAction = new AbstractAction(org.openpnp.Translations.getString("Local.781f621de8fc6431"), Icons.contactProbeNozzle) {
        {
            putValue(Action.SHORT_DESCRIPTION, org.openpnp.Translations.getString("Local.7f68823c7a5be522"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                nozzle.calibrateZ(nozzle.getCalibrationNozzleTip());
            });
        }
    };

    private JPanel panel;
    private JLabel lblContactProbeActuator;
    private JComboBox comboBoxContactProbeActuator;
    private JLabel lblStartOffset;
    private JTextField contactProbeStartOffsetZ;
    private JLabel lblProbeDepth;
    private JTextField contactProbeDepthZ;
    private JLabel lblSniffleIncrement;
    private JTextField sniffleIncrementZ;
    private JLabel lblFinalAdjustment;
    private JTextField contactProbeAdjustZ;
    private JLabel lblMethod;
    private JComboBox contactProbeMethod;
    private JLabel lblFeederHeightProbing;
    private JComboBox feederHeightProbing;
    private JLabel lblPartHeightProbing;
    private JComboBox partHeightProbing;
    private JLabel lblZCalibration;
    private JTextField calibrationOffsetZ;
    private JButton btnCalibrateNow;
    private JLabel lblSniffleDwellTime;
    private JTextField sniffleDwellTime;
    private JLabel lblDiscardProbing;
    private JCheckBox discardProbing;
    private JLabel lblProbeSpeed;
    private JTextField contactProbeSpeed;

}
