/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.gui.support.PercentConverter;
import org.openpnp.machine.reference.ActuatorInterlockMonitor;
import org.openpnp.machine.reference.ActuatorInterlockMonitor.InterlockType;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ActuatorInterlockMonitorConfigurationWizard extends AbstractConfigurationWizard {
    private AbstractActuator actuator;
    private ActuatorInterlockMonitor monitor;
    
    private JLabel lblAxis1;
    private JComboBox interlockAxis1;
    private JLabel lblAxis2;
    private JComboBox interlockAxis2;
    private JLabel lblAxis;
    private JLabel lblAxis_1;
    private JComboBox interlockAxis3;
    private JComboBox interlockAxis4;
    private JPanel panelInterlock;
    private JLabel lblConfirmationGoodRange;
    private JTextField confirmationGoodMin;
    private JTextField confirmationGoodMax;
    private JLabel lblConfirmationPattern;
    private JTextField confirmationPattern;
    private JLabel lblRegex;
    private JCheckBox confirmationByRegex;
    private JLabel lblActuator;
    private JComboBox conditionalActuator;
    private JLabel lblSpeed;
    private JTextField conditionalSpeedPercentMin;
    private JTextField conditionalSpeedPercentMax;
    private JSeparator separator;
    private JComboBox conditionalActuatorState;
    private JPanel panelCondition;
    private JLabel lblFunction;
    private JComboBox interlockType;

    public ActuatorInterlockMonitorConfigurationWizard(AbstractMachine machine, AbstractActuator actuator, ActuatorInterlockMonitor monitor) {
        super();
        this.monitor = monitor;
        this.actuator = actuator;
        createUi(machine);
    }

    protected void createUi(AbstractMachine machine) {
        panelInterlock = new JPanel();
        panelInterlock.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.921adb00b4c7d11a"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelInterlock);
        panelInterlock.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
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
                FormSpecs.DEFAULT_ROWSPEC,}));
                
                lblFunction = new JLabel(org.openpnp.Translations.getString("Local.ba96bb3284dbf8a5"));
                panelInterlock.add(lblFunction, "2, 2, right, default");
                
                interlockType = new org.openpnp.gui.components.LocalizedComboBox(ActuatorInterlockMonitor.InterlockType.values());
                interlockType.setMaximumRowCount(15);
                interlockType.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        adaptDialog();
                    }
                });
                panelInterlock.add(interlockType, "4, 2, 3, 1");
        
                lblConfirmationGoodRange = new JLabel(org.openpnp.Translations.getString("Local.14dfdc5f2f83c753"));
                lblConfirmationGoodRange.setToolTipText(org.openpnp.Translations.getString("Local.901b810bbacc6338"));
                panelInterlock.add(lblConfirmationGoodRange, "2, 4, right, default");
        
                confirmationGoodMin = new JTextField();
                panelInterlock.add(confirmationGoodMin, "4, 4, fill, default");
                confirmationGoodMin.setColumns(10);
        
                confirmationGoodMax = new JTextField();
                panelInterlock.add(confirmationGoodMax, "6, 4, fill, default");
                confirmationGoodMax.setColumns(10);
        
                lblConfirmationPattern = new JLabel(org.openpnp.Translations.getString("Local.e9fb33fa27493639"));
                lblConfirmationPattern.setToolTipText(org.openpnp.Translations.getString("Local.b1d938f61f5f9192"));
                panelInterlock.add(lblConfirmationPattern, "2, 6, right, default");
        
                confirmationPattern = new JTextField();
                panelInterlock.add(confirmationPattern, "4, 6, 3, 1, fill, default");
                confirmationPattern.setColumns(10);
        
                lblRegex = new JLabel(org.openpnp.Translations.getString("Local.3e191031c3231f85"));
                lblRegex.setToolTipText(org.openpnp.Translations.getString("Local.b3c8a732e17cff14"));
                panelInterlock.add(lblRegex, "8, 6, right, default");
        
                confirmationByRegex = new JCheckBox("");
                panelInterlock.add(confirmationByRegex, "10, 6");

        lblAxis1 = new JLabel(org.openpnp.Translations.getString("Local.bbe8efb2ad193493"));
        panelInterlock.add(lblAxis1, "2, 10, right, default");

        interlockAxis1 = new org.openpnp.gui.components.LocalizedComboBox(new AxesComboBoxModel(machine, CoordinateAxis.class, null, true));
        panelInterlock.add(interlockAxis1, "4, 10, 3, 1, fill, default");

        lblAxis2 = new JLabel(org.openpnp.Translations.getString("Local.97edc23d42a95a73"));
        panelInterlock.add(lblAxis2, "2, 12, right, default");

        interlockAxis2 = new org.openpnp.gui.components.LocalizedComboBox(new AxesComboBoxModel(machine, CoordinateAxis.class, null, true));
        panelInterlock.add(interlockAxis2, "4, 12, 3, 1, fill, default");

        lblAxis = new JLabel(org.openpnp.Translations.getString("Local.fbaaa6f5d1178a63"));
        panelInterlock.add(lblAxis, "2, 14, right, default");

        interlockAxis3 = new org.openpnp.gui.components.LocalizedComboBox(new AxesComboBoxModel(machine, CoordinateAxis.class, null, true));
        panelInterlock.add(interlockAxis3, "4, 14, 3, 1, fill, default");

        lblAxis_1 = new JLabel(org.openpnp.Translations.getString("Local.cee796b19ac4f196"));
        panelInterlock.add(lblAxis_1, "2, 16, right, default");

        interlockAxis4 = new org.openpnp.gui.components.LocalizedComboBox(new AxesComboBoxModel(machine, CoordinateAxis.class, null, true));
        panelInterlock.add(interlockAxis4, "4, 16, 3, 1, fill, default");
        
        panelCondition = new JPanel();
        panelCondition.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.df32040ef9dddd19"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCondition);
        panelCondition.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblActuator = new JLabel(org.openpnp.Translations.getString("Local.e5de8b822058cd7a"));
        lblActuator.setToolTipText(org.openpnp.Translations.getString("Local.62b171fbdbd856de"));
        panelCondition.add(lblActuator, "2, 2, right, default");
        
        conditionalActuator = new org.openpnp.gui.components.LocalizedComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        conditionalActuator.setMaximumRowCount(15);
        conditionalActuator.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelCondition.add(conditionalActuator, "4, 2, 3, 1, fill, default");
        
        conditionalActuatorState = new org.openpnp.gui.components.LocalizedComboBox(ActuatorInterlockMonitor.ActuatorState.values());
        panelCondition.add(conditionalActuatorState, "8, 2, 3, 1, fill, default");
        
        lblSpeed = new JLabel(org.openpnp.Translations.getString("Local.aa3c0fa1446b59e2"));
        lblSpeed.setToolTipText(org.openpnp.Translations.getString("Local.9197f6408855d814"));
        panelCondition.add(lblSpeed, "2, 4, right, default");
        
        conditionalSpeedPercentMin = new JTextField();
        panelCondition.add(conditionalSpeedPercentMin, "4, 4, fill, default");
        conditionalSpeedPercentMin.setColumns(10);
        
        conditionalSpeedPercentMax = new JTextField();
        panelCondition.add(conditionalSpeedPercentMax, "6, 4, fill, default");
        conditionalSpeedPercentMax.setColumns(10);
    }

    protected void adaptDialog() {
        ActuatorInterlockMonitor.InterlockType interlockType = (InterlockType) this.interlockType.getSelectedItem();
        boolean hasRange = (interlockType != null && interlockType.isReadingDouble());
        boolean hasPattern = (interlockType != null && interlockType.isReadingString());
        
        lblConfirmationGoodRange.setVisible(hasRange);
        confirmationGoodMin.setVisible(hasRange);
        confirmationGoodMax.setVisible(hasRange);

        lblConfirmationPattern.setVisible(hasPattern);
        confirmationPattern.setVisible(hasPattern);
        lblRegex.setVisible(hasPattern);
        confirmationByRegex.setVisible(hasPattern);

        conditionalActuatorState.setVisible(conditionalActuator.getSelectedItem() != null);
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");//Configuration.get().getLengthDisplayFormat());
        PercentConverter percentConverter =
                new PercentConverter();
        NamedConverter<Driver> driverConverter = new NamedConverter<>(machine.getDrivers()); 
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 
        NamedConverter<Actuator> actuatorConverter = new NamedConverter<>((actuator.getHead() != null ? 
                actuator.getHead().getActuators() : machine.getActuators())); 

        addWrappedBinding(monitor, "interlockType", interlockType, "selectedItem");

        addWrappedBinding(monitor, "interlockAxis1", interlockAxis1, "selectedItem", axisConverter);
        addWrappedBinding(monitor, "interlockAxis2", interlockAxis2, "selectedItem", axisConverter);
        addWrappedBinding(monitor, "interlockAxis3", interlockAxis3, "selectedItem", axisConverter);
        addWrappedBinding(monitor, "interlockAxis4", interlockAxis4, "selectedItem", axisConverter);
        addWrappedBinding(monitor, "confirmationGoodMin", confirmationGoodMin, "text", doubleConverter);
        addWrappedBinding(monitor, "confirmationGoodMax", confirmationGoodMax, "text", doubleConverter);
        addWrappedBinding(monitor, "confirmationPattern", confirmationPattern, "text");
        addWrappedBinding(monitor, "confirmationByRegex", confirmationByRegex, "selected");
        addWrappedBinding(monitor, "conditionalActuator", conditionalActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(monitor, "conditionalActuatorState", conditionalActuatorState, "selectedItem");
        addWrappedBinding(monitor, "conditionalSpeedMin", conditionalSpeedPercentMin, "text", percentConverter);
        addWrappedBinding(monitor, "conditionalSpeedMax", conditionalSpeedPercentMax, "text", percentConverter);

        ComponentDecorators.decorateWithAutoSelect(confirmationGoodMin);
        ComponentDecorators.decorateWithAutoSelect(confirmationGoodMax);
        ComponentDecorators.decorateWithAutoSelect(confirmationPattern);

        adaptDialog();
    }
}
