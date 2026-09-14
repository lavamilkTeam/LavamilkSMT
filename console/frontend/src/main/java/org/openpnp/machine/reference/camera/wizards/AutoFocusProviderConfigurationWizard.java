/*
 * Copyright (C) 2021 <mark@makr.zone>
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

package org.openpnp.machine.reference.camera.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.camera.AutoFocusProvider;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class AutoFocusProviderConfigurationWizard extends AbstractConfigurationWizard {
    private AutoFocusProvider focusProvider;
    private Camera camera;

    public AutoFocusProviderConfigurationWizard(Camera camera, FocusProvider focusProvider) {
        this.camera = camera;
        this.focusProvider = (AutoFocusProvider) focusProvider;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.894b72c7d984b231"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblFocalResolution = new JLabel(org.openpnp.Translations.getString("Local.f9936fd874b68112"));
        lblFocalResolution.setToolTipText(org.openpnp.Translations.getString("Local.7d03545d1f0130ef"));
        panelGeneral.add(lblFocalResolution, "2, 2, right, default");

        focalResolution = new JTextField();
        panelGeneral.add(focalResolution, "4, 2, fill, default");
        focalResolution.setColumns(10);

        btnTest = new JButton(testAutoFocusAction);
        panelGeneral.add(btnTest, "6, 2, 1, 7");

        lblAveragedFrames = new JLabel(org.openpnp.Translations.getString("Local.0d13ea3cca6cab82"));
        lblAveragedFrames.setToolTipText(org.openpnp.Translations.getString("Local.9e636a348d11cad2"));
        panelGeneral.add(lblAveragedFrames, "2, 4, right, default");

        averagedFrames = new JTextField();
        panelGeneral.add(averagedFrames, "4, 4, fill, default");
        averagedFrames.setColumns(10);

        lblFocusSpeed = new JLabel(org.openpnp.Translations.getString("Local.f1ad23cfd9e97d0a"));
        lblFocusSpeed.setToolTipText(org.openpnp.Translations.getString("Local.f00726640273d80c"));
        panelGeneral.add(lblFocusSpeed, "2, 6, right, default");

        focusSpeed = new JTextField();
        panelGeneral.add(focusSpeed, "4, 6, fill, default");
        focusSpeed.setColumns(10);

        lblShowDiagnostics = new JLabel(org.openpnp.Translations.getString("Local.f54af666d7128541"));
        lblShowDiagnostics.setToolTipText(org.openpnp.Translations.getString("Local.cc476e47ec53c138"));
        panelGeneral.add(lblShowDiagnostics, "2, 8, right, default");

        showDiagnostics = new JCheckBox("");
        panelGeneral.add(showDiagnostics, "4, 8");

        lblLastFocusDistance = new JLabel(org.openpnp.Translations.getString("Local.62b93187c0d9512b"));
        panelGeneral.add(lblLastFocusDistance, "2, 12, right, default");

        txtLastFocusDistance = new JTextField();
        txtLastFocusDistance.setEditable(false);
        panelGeneral.add(txtLastFocusDistance, "4, 12, fill, default");
        txtLastFocusDistance.setColumns(10);
        
                btnSetCameraZ = new JButton(adjustCameraZAction);
                panelGeneral.add(btnSetCameraZ, "6, 12");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(focusProvider, "focalResolution", focalResolution, "text", lengthConverter);
        addWrappedBinding(focusProvider, "averagedFrames", averagedFrames, "text", intConverter);
        addWrappedBinding(focusProvider, "focusSpeed", focusSpeed, "text", doubleConverter);
        addWrappedBinding(focusProvider, "showDiagnostics", showDiagnostics, "selected");

        addWrappedBinding(this, "lastFocusDistance", txtLastFocusDistance, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(focalResolution);
        ComponentDecorators.decorateWithAutoSelect(averagedFrames);
        ComponentDecorators.decorateWithAutoSelect(focusSpeed);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(txtLastFocusDistance);
    }

    public Length getLastFocusDistance() {
        return lastFocusDistance;
    }

    public void setLastFocusDistance(Length lastFocusDistance) {
        this.lastFocusDistance = lastFocusDistance;
        firePropertyChange("lastFocusDistance", null, lastFocusDistance);
    }

    private Action testAutoFocusAction = new AbstractAction("", Icons.centerPin) {
        {
            putValue(Action.SHORT_DESCRIPTION, org.openpnp.Translations.getString("Local.c120135534c0abad"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            camera.ensureCameraVisible();
            UiUtils.submitUiMachineTask(() -> {
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                if (!(nozzle instanceof ReferenceNozzle)) {
                    throw new Exception(org.openpnp.Translations.format("Local.1b2b390358e36366", (nozzle.getName())));
                }
                ReferenceNozzleTip nt = ((ReferenceNozzle) nozzle).getCalibrationNozzleTip(); 
                if (nt == null) {
                    throw new Exception(org.openpnp.Translations.getString("Local.3aac744b907cd04d"));
                }
                Location location1 = camera.getLocation(nozzle)
                        .derive(nozzle.getLocation(), false, false, false, true); // Keep rotation
                Length maxPartHeight = nt.getMaxPartHeight();
                Location location0 = location1.add(new Location(maxPartHeight.getUnits(), 
                        0, 0, maxPartHeight.getValue(), 0));
                Location focus = focusProvider.autoFocus(camera, nozzle, nt.getMaxPartDiameter()
                .add(nt.getMaxPickTolerance().multiply(2.0)), location0, location1);
                setLastFocusDistance(focus.getXyzLengthTo(location1));
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
    };

    private Action adjustCameraZAction = new AbstractAction(org.openpnp.Translations.getString("Local.c40e4f8b9045da88")) {
        {
            putValue(Action.SHORT_DESCRIPTION, org.openpnp.Translations.getString("Local.8986290869a32aaf"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> {
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                if (nozzle.getPart() != null) {
                    throw new Exception(org.openpnp.Translations.format("Local.6135e966f06e92f5", (nozzle.getName())));
                }
                if (!(nozzle instanceof ReferenceNozzle)) {
                    throw new Exception(org.openpnp.Translations.format("Local.1b2b390358e36366", (nozzle.getName())));
                }
                if (!(camera instanceof ReferenceCamera)) {
                    throw new Exception(org.openpnp.Translations.format("Local.d0d4790eec071b81", (camera.getName())));
                }
                if (camera.getLocation(nozzle).getLinearLengthTo(nozzle.getLocation()).convertToUnits(LengthUnit.Millimeters).getValue() > 0.1) {
                    throw new Exception(org.openpnp.Translations.format("Local.7f46ae9884de37d9", (nozzle.getName())));
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        org.openpnp.Translations.getString("Local.0b223c7137e88e5f"),
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    Length offsetZ = camera.getLocation(nozzle).getLengthZ().subtract(nozzle.getLocation().getLengthZ());
                    Location cameraHeadOffsets = ((ReferenceCamera) camera).getHeadOffsets();
                    Location cameraHeadOffsetsNew = cameraHeadOffsets.subtract(new Location(offsetZ.getUnits(), 0, 0, offsetZ.getValue(), 0));
                    Logger.info("Setting camera "+camera.getName()+" Z to "+cameraHeadOffsetsNew.getLengthZ()+" (previously "+cameraHeadOffsets.getLengthZ());
                    ((ReferenceCamera) camera).setHeadOffsets(cameraHeadOffsetsNew);
                    setLastFocusDistance(null);
                    MovableUtils.fireTargetedUserAction(camera);
                }
            });
        }
    };

    private JPanel panelGeneral;
    private JLabel lblFocalResolution;
    private JTextField focalResolution;
    private JLabel lblAveragedFrames;
    private JTextField averagedFrames;
    private JLabel lblFocusSpeed;
    private JTextField focusSpeed;
    private JButton btnTest;
    private JLabel lblLastFocusDistance;
    private JTextField txtLastFocusDistance;

    private Length lastFocusDistance;
    private JLabel lblShowDiagnostics;
    private JCheckBox showDiagnostics;
    private JButton btnSetCameraZ;
}
