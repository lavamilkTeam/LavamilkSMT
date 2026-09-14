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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.SimulationModeMachine.SimulationMode;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SimulationModeMachineConfigurationWizard extends AbstractConfigurationWizard {

    private final SimulationModeMachine machine;
    private JTextField homingErrorX;
    private JTextField homingErrorY;
    private JTextField simulatedNonSquarenessFactor;
    private JTextField simulatedRunout;
    private JTextField simulatedCameraNoise;
    private JTextField simulatedVibrationAmplitude;
    private JComboBox simulationMode;
    private JTextField simulatedRunoutPhase;
    private JCheckBox pickAndPlaceChecking;
    private JTextField simulatedCameraLag;
    private JTextField machineTableZ;
    private JTextField simulatedVibrationDuration;
    private JCheckBox replacingDrivers;

    public SimulationModeMachineConfigurationWizard(SimulationModeMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.c910d474dcd724bf"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSimulationMode = new JLabel(org.openpnp.Translations.getString("Local.05ec7bc6df5dbfd4"));
        panelGeneral.add(lblSimulationMode, "2, 2, right, default");

        simulationMode = new org.openpnp.gui.components.LocalizedComboBox(SimulationMode.values());
        panelGeneral.add(simulationMode, "4, 2, fill, default");
        
        JLabel lblReplaceDrivers = new JLabel(org.openpnp.Translations.getString("Local.46c100412e22c83e"));
        lblReplaceDrivers.setToolTipText(org.openpnp.Translations.getString("Local.961ffc36ee76ca9c"));
        panelGeneral.add(lblReplaceDrivers, "2, 4, right, default");
        
        replacingDrivers = new JCheckBox("");
        panelGeneral.add(replacingDrivers, "4, 4");

        JPanel panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.8bac3433650a8cfe"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(100dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));

        JLabel lblNozzleTipRunout = new JLabel(org.openpnp.Translations.getString("Local.5f8f14090b873372"));
        lblNozzleTipRunout.setToolTipText(org.openpnp.Translations.getString("Local.f546021378ad8890"));
        panelLocations.add(lblNozzleTipRunout, "2, 2, right, default");

        simulatedRunout = new JTextField();
        panelLocations.add(simulatedRunout, "4, 2");
        simulatedRunout.setColumns(10);
        
        JLabel lblWarnRunout = new JLabel(org.openpnp.Translations.getString("Local.7cc1952bc8691515"));
        panelLocations.add(lblWarnRunout, "6, 2, 3, 5, fill, top");
        
        JLabel lblRunoutPhase = new JLabel(org.openpnp.Translations.getString("Local.f543a229d361b3de"));
        lblRunoutPhase.setToolTipText(org.openpnp.Translations.getString("Local.30a681edb72e2e31"));
        panelLocations.add(lblRunoutPhase, "2, 4, right, default");
        
        simulatedRunoutPhase = new JTextField();
        panelLocations.add(simulatedRunoutPhase, "4, 4, fill, default");
        simulatedRunoutPhase.setColumns(10);
        
                JLabel lblNonsquarenessFactor = new JLabel(org.openpnp.Translations.getString("Local.2c05d5dbebb0bf71"));
                lblNonsquarenessFactor.setToolTipText(org.openpnp.Translations.getString("Local.395138ee9f7d93e3"));
                panelLocations.add(lblNonsquarenessFactor, "2, 8, right, default");
        
                simulatedNonSquarenessFactor = new JTextField();
                panelLocations.add(simulatedNonSquarenessFactor, "4, 8");
                simulatedNonSquarenessFactor.setColumns(10);
        
        JLabel lblPickPlace = new JLabel(org.openpnp.Translations.getString("Local.be93c1b1dfb1dd8b"));
        panelLocations.add(lblPickPlace, "2, 12, right, default");
        
        pickAndPlaceChecking = new JCheckBox("");
        panelLocations.add(pickAndPlaceChecking, "4, 12");
        
        JLabel lblCameraLags = new JLabel(org.openpnp.Translations.getString("Local.7a953ba01f8d9536"));
        panelLocations.add(lblCameraLags, "2, 16, right, default");
        
        simulatedCameraLag = new JTextField();
        panelLocations.add(simulatedCameraLag, "4, 16, fill, default");
        simulatedCameraLag.setColumns(10);
        
                JLabel lblCameraNoise = new JLabel(org.openpnp.Translations.getString("Local.8d266f5cd6330d69"));
                lblCameraNoise.setToolTipText(org.openpnp.Translations.getString("Local.be8358c84abe10f3"));
                panelLocations.add(lblCameraNoise, "2, 18, right, default");
        
                simulatedCameraNoise = new JTextField();
                panelLocations.add(simulatedCameraNoise, "4, 18");
                simulatedCameraNoise.setColumns(10);

        JLabel lblVibrationAmplitude = new JLabel(org.openpnp.Translations.getString("Local.9366197fb67bf98c"));
        lblVibrationAmplitude.setToolTipText(org.openpnp.Translations.getString("Local.28f201192c032f70"));
        panelLocations.add(lblVibrationAmplitude, "2, 20, right, default");

        simulatedVibrationAmplitude = new JTextField();
        panelLocations.add(simulatedVibrationAmplitude, "4, 20, fill, default");
        simulatedVibrationAmplitude.setColumns(10);
        
        JLabel lblDuration = new JLabel(org.openpnp.Translations.getString("Local.cb259afc5adfa2de"));
        lblDuration.setToolTipText(org.openpnp.Translations.getString("Local.40c643ca31b5f6cc"));
        panelLocations.add(lblDuration, "2, 22, right, default");
        
        simulatedVibrationDuration = new JTextField();
        panelLocations.add(simulatedVibrationDuration, "4, 22, left, default");
        simulatedVibrationDuration.setColumns(10);

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 26");
        lblX.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 26");
        lblY.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblDiscardPoint = new JLabel(org.openpnp.Translations.getString("Local.3a00fc1dcc27e502"));
        lblDiscardPoint.setToolTipText(org.openpnp.Translations.getString("Local.4fa5c567dd927478"));
        panelLocations.add(lblDiscardPoint, "2, 28, right, default");

        homingErrorX = new JTextField();
        panelLocations.add(homingErrorX, "4, 28");
        homingErrorX.setColumns(10);

        homingErrorY = new JTextField();
        panelLocations.add(homingErrorY, "6, 28");
        homingErrorY.setColumns(10);
        
        JButton btnResetFeeders = new JButton(org.openpnp.Translations.getString("Local.32df903e34ecc9e9"));
        btnResetFeeders.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                machine.resetAllFeeders();
            }
        });
        
        JLabel lblMachineTableZ = new JLabel(org.openpnp.Translations.getString("Local.2f8519b295ecf31c"));
        panelLocations.add(lblMachineTableZ, "2, 30, right, default");
        
        machineTableZ = new JTextField();
        panelLocations.add(machineTableZ, "4, 30, fill, default");
        machineTableZ.setColumns(10);
        
        JButton btnSetMachineTable = new JButton(org.openpnp.Translations.getString("Local.ca916b8a55926d1c"));
        btnSetMachineTable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LengthConverter lengthConverter = new LengthConverter();
                Length tableZ = lengthConverter.convertReverse(machineTableZ.getText());
                machine.setMachineTableZ(tableZ);
            }
        });
        panelLocations.add(btnSetMachineTable, "8, 30");
        panelLocations.add(btnResetFeeders, "8, 32");
        
        JLabel label = new JLabel(" ");
        panelLocations.add(label, "2, 34");
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");
        DoubleConverter degreeConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter =
                new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(machine, "simulationMode", simulationMode, "selectedItem");
        addWrappedBinding(machine, "replacingDrivers", replacingDrivers, "selected");

        addWrappedBinding(machine, "simulatedNonSquarenessFactor", simulatedNonSquarenessFactor, "text", doubleConverter);

        addWrappedBinding(machine, "simulatedRunout", simulatedRunout, "text", lengthConverter);
        addWrappedBinding(machine, "simulatedRunoutPhase", simulatedRunoutPhase, "text", degreeConverter);
        addWrappedBinding(machine, "pickAndPlaceChecking", pickAndPlaceChecking, "selected");

        addWrappedBinding(machine, "simulatedVibrationAmplitude", simulatedVibrationAmplitude, "text", doubleConverter);
        addWrappedBinding(machine, "simulatedVibrationDuration", simulatedVibrationDuration, "text", doubleConverter);
        addWrappedBinding(machine, "simulatedCameraNoise", simulatedCameraNoise, "text", integerConverter);
        addWrappedBinding(machine, "simulatedCameraLag", simulatedCameraLag, "text", doubleConverter);

        MutableLocationProxy homingError = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "homingError", homingError, "location");
        addWrappedBinding(homingError, "lengthX", homingErrorX, "text", lengthConverter);
        addWrappedBinding(homingError, "lengthY", homingErrorY, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(machineTableZ);
    }
}
