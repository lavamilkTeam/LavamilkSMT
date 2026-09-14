/*
 * Copyright (C) 2020 <mark@makr.zone>
 * based on the ReferenceLeverFeeder 
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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.processes.RegionOfInterestProcess;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder.OcrWrongPartAction;
import org.openpnp.model.Configuration;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OcrUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferencePushPullFeederConfigurationWizard
extends AbstractReferenceFeederConfigurationWizard {
    private final ReferencePushPullFeeder feeder;

    public ReferencePushPullFeederConfigurationWizard(ReferencePushPullFeeder feeder) {
        super(feeder, false);
        this.feeder = feeder;

        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.95c61170318a2851"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(26dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:min"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        btnShowVisionFeatures = new JButton(showVisionFeaturesAction);
        btnShowVisionFeatures.setToolTipText(org.openpnp.Translations.getString("Local.9016b7ad49d27c06"));
        btnShowVisionFeatures.setText(org.openpnp.Translations.getString("Local.2d0b0eb1bf5e23bc"));
        panelLocations.add(btnShowVisionFeatures, "2, 2, default, fill");

        btnAutoSetup = new JButton(autoSetupAction);
        panelLocations.add(btnAutoSetup, "4, 2, 5, 1");
        
                button = new JButton(plusOneAction);
                panelLocations.add(button, "10, 2");

        lblX_1 = new JLabel("X");
        panelLocations.add(lblX_1, "4, 4");

        lblY_1 = new JLabel("Y");
        panelLocations.add(lblY_1, "6, 4");

        lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 4");

        lblPickLocation = new JLabel(org.openpnp.Translations.getString("Local.54d1e4e55a495b96"));
        lblPickLocation.setToolTipText(org.openpnp.Translations.getString("Local.fa6c44cf164b7077"));
        panelLocations.add(lblPickLocation, "2, 6, right, default");

        textFieldPickLocationX = new JTextField();
        panelLocations.add(textFieldPickLocationX, "4, 6");
        textFieldPickLocationX.setColumns(10);

        textFieldPickLocationY = new JTextField();
        panelLocations.add(textFieldPickLocationY, "6, 6");
        textFieldPickLocationY.setColumns(10);

        textFieldPickLocationZ = new JTextField();
        panelLocations.add(textFieldPickLocationZ, "8, 6");
        textFieldPickLocationZ.setColumns(10);

        locationButtonsPanelFirstPick = new LocationButtonsPanel(textFieldPickLocationX, textFieldPickLocationY, textFieldPickLocationZ, null);
        panelLocations.add(locationButtonsPanelFirstPick, "10, 6");

        lblNormalizePickLocation = new JLabel(org.openpnp.Translations.getString("Local.c8dc58be86a8bee1"));
        lblNormalizePickLocation.setToolTipText(org.openpnp.Translations.getString("Local.1d3a13d59ed01623"));
        panelLocations.add(lblNormalizePickLocation, "2, 8, right, default");

        checkBoxNormalizePickLocation = new JCheckBox("");
        panelLocations.add(checkBoxNormalizePickLocation, "4, 8");
        checkBoxNormalizePickLocation.setSelected(true);

        lblHole1Location = new JLabel(org.openpnp.Translations.getString("Local.b5a267fa164781d4"));
        lblHole1Location.setToolTipText(org.openpnp.Translations.getString("Local.7df61ddc90d822e7"));
        panelLocations.add(lblHole1Location, "2, 10, right, default");

        textFieldHole1LocationX = new JTextField();
        panelLocations.add(textFieldHole1LocationX, "4, 10");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelLocations.add(textFieldHole1LocationY, "6, 10");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole1, "10, 10");

        lblHole2Location = new JLabel(org.openpnp.Translations.getString("Local.a077d6bf44f0b295"));
        lblHole2Location.setToolTipText(org.openpnp.Translations.getString("Local.80fd96d2fe2ee95b"));
        panelLocations.add(lblHole2Location, "2, 12, right, default");

        textFieldHole2LocationX = new JTextField();
        panelLocations.add(textFieldHole2LocationX, "4, 12");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelLocations.add(textFieldHole2LocationY, "6, 12");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelLocations.add(locationButtonsPanelHole2, "10, 12");

        lblSnapToAxis = new JLabel(org.openpnp.Translations.getString("Local.2047fd7f45192718"));
        lblSnapToAxis.setToolTipText(org.openpnp.Translations.getString("Local.891516249ede9fbb"));
        panelLocations.add(lblSnapToAxis, "2, 14, right, default");

        checkBoxSnapToAxis = new JCheckBox("");
        checkBoxSnapToAxis.setToolTipText(org.openpnp.Translations.getString("Local.891516249ede9fbb"));
        panelLocations.add(checkBoxSnapToAxis, "4, 14");
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.6b705815b8fa1fc0"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelTape = new JPanel();
        contentPanel.add(panelTape);
        panelTape.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.6b705815b8fa1fc0"), TitledBorder.LEADING, TitledBorder.TOP, null));
        panelTape.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(26dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblPartPitch = new JLabel(org.openpnp.Translations.getString("Local.74bb859d236619ad"));
        panelTape.add(lblPartPitch, "2, 2, right, default");
        lblPartPitch.setToolTipText(org.openpnp.Translations.getString("Local.fa12347ab2c5b3d0"));

        textFieldPartPitch = new JTextField();
        panelTape.add(textFieldPartPitch, "4, 2");
        textFieldPartPitch.setToolTipText(org.openpnp.Translations.getString("Local.fa12347ab2c5b3d0"));
        textFieldPartPitch.setColumns(5);

        lblRotation = new JLabel(org.openpnp.Translations.getString("Local.3287c2524d45dc15"));
        panelTape.add(lblRotation, "6, 2, right, default");
        lblRotation.setToolTipText(org.openpnp.Translations.getString("Local.0163ac6fc01d63f5"));

        textFieldRotationInTape = new JTextField();
        panelTape.add(textFieldRotationInTape, "8, 2");
        textFieldRotationInTape.setToolTipText(org.openpnp.Translations.getString("Local.5acb2275bbc54188"));
        textFieldRotationInTape.setColumns(10);

        lblFeedPitch = new JLabel(org.openpnp.Translations.getString("Local.21a76a183c8996ff"));
        panelTape.add(lblFeedPitch, "2, 4, right, default");
        lblFeedPitch.setToolTipText(org.openpnp.Translations.getString("Local.15631a2eced5c0cf"));

        textFieldFeedPitch = new JTextField();
        panelTape.add(textFieldFeedPitch, "4, 4");
        textFieldFeedPitch.setToolTipText(org.openpnp.Translations.getString("Local.15631a2eced5c0cf"));
        textFieldFeedPitch.setColumns(10);

        lblMultiplier = new JLabel(org.openpnp.Translations.getString("Local.88fb11573a6a0105"));
        panelTape.add(lblMultiplier, "6, 4, right, default");
        lblMultiplier.setToolTipText(org.openpnp.Translations.getString("Local.164ad3ecdf2b1243"));

        textFieldFeedMultiplier = new JTextField();
        panelTape.add(textFieldFeedMultiplier, "8, 4");
        textFieldFeedMultiplier.setToolTipText(org.openpnp.Translations.getString("Local.164ad3ecdf2b1243"));
        textFieldFeedMultiplier.setColumns(10);

        btnDiscardParts = new JButton(discardPartsAction);
        btnDiscardParts.setToolTipText(org.openpnp.Translations.getString("Local.198e43aa0dbbab48"));
        panelTape.add(btnDiscardParts, "10, 4");

        lblFeedCount = new JLabel(org.openpnp.Translations.getString("Local.17a082c38cac4aec"));
        panelTape.add(lblFeedCount, "6, 6, right, default");
        lblFeedCount.setToolTipText(org.openpnp.Translations.getString("Local.4cef8bc696e36d8e"));

        textFieldFeedCount = new JTextField();
        panelTape.add(textFieldFeedCount, "8, 6");
        textFieldFeedCount.setToolTipText(org.openpnp.Translations.getString("Local.4cef8bc696e36d8e"));
        textFieldFeedCount.setColumns(10);

        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "10, 6");

        Head head = null;
        try {
            head = Configuration.get().getMachine().getDefaultHead();
        }
        catch (Exception e) {
            Logger.error(e, "Cannot determine default head of machine.");
        }

        //
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.c587c2601ccfc456"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new BoxLayout(panelVision, BoxLayout.Y_AXIS));

        panelVisionEnabled = new JPanel();
        panelVision.add(panelVisionEnabled);
        panelVisionEnabled.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.LINE_GAP_ROWSPEC,
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

        lblCalibrationTrigger = new JLabel(org.openpnp.Translations.getString("Local.869e8bdd5c350a5c"));
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 2, right, default");

        comboBoxCalibrationTrigger = new org.openpnp.gui.components.LocalizedComboBox(ReferencePushPullFeeder.CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 2");

        lblPrecisionAverage = new JLabel(org.openpnp.Translations.getString("Local.c54737a6dcbf95b9"));
        lblPrecisionAverage.setToolTipText(org.openpnp.Translations.getString("Local.ed6d8d3615fc4892"));
        panelVisionEnabled.add(lblPrecisionAverage, "8, 2, right, default");

        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText(org.openpnp.Translations.getString("Local.ed6d8d3615fc4892"));
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "10, 2");
        textFieldPrecisionAverage.setColumns(10);

        lblCalibrationCount = new JLabel(org.openpnp.Translations.getString("Local.398f556bdbdd9d08"));
        panelVisionEnabled.add(lblCalibrationCount, "12, 2, right, default");

        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "14, 2");
        textFieldCalibrationCount.setColumns(10);

        lblPrecisionWanted = new JLabel(org.openpnp.Translations.getString("Local.a22afc480274ac30"));
        lblPrecisionWanted.setToolTipText(org.openpnp.Translations.getString("Local.d601eb8aa43513ce"));
        panelVisionEnabled.add(lblPrecisionWanted, "2, 4, right, default");

        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText(org.openpnp.Translations.getString("Local.d601eb8aa43513ce"));
        panelVisionEnabled.add(textFieldPrecisionWanted, "4, 4");
        textFieldPrecisionWanted.setColumns(10);

        lblPrecisionConfidenceLimit = new JLabel(org.openpnp.Translations.getString("Local.cda43a020c277588"));
        lblPrecisionConfidenceLimit.setToolTipText(org.openpnp.Translations.getString("Local.855e19306851665e"));
        panelVisionEnabled.add(lblPrecisionConfidenceLimit, "8, 4, right, default");

        textFieldPrecisionConfidenceLimit = new JTextField();
        textFieldPrecisionConfidenceLimit.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionConfidenceLimit, "10, 4");
        textFieldPrecisionConfidenceLimit.setColumns(10);

        btnResetStatistics = new JButton(resetStatisticsAction);
        panelVisionEnabled.add(btnResetStatistics, "12, 4, 3, 1");

        lblOcrWrongPart = new JLabel(org.openpnp.Translations.getString("Local.e17575e5e5a58032"));
        lblOcrWrongPart.setToolTipText(org.openpnp.Translations.getString("Local.7a389ab45b4df91e"));
        panelVisionEnabled.add(lblOcrWrongPart, "2, 8, right, default");

        comboBoxWrongPartAction = new org.openpnp.gui.components.LocalizedComboBox(ReferencePushPullFeeder.OcrWrongPartAction.values());
        panelVisionEnabled.add(comboBoxWrongPartAction, "4, 8");

        List<String> fontList = OcrUtils.createFontSelectionList(feeder.getOcrFontName(), true);

        lblOcrFontName = new JLabel(org.openpnp.Translations.getString("Local.23b30e24a1c568f5"));
        lblOcrFontName.setToolTipText(org.openpnp.Translations.getString("Local.4db4fe2c55e5722e"));
        panelVisionEnabled.add(lblOcrFontName, "8, 8, right, default");
        comboBoxFontName = new org.openpnp.gui.components.LocalizedComboBox(fontList.toArray());
        panelVisionEnabled.add(comboBoxFontName, "10, 8");

        btnSetupocrregion = new JButton(setupOcrRegionAction);
        panelVisionEnabled.add(btnSetupocrregion, "12, 8, 3, 1");

        lblStopAfterWrong = new JLabel(org.openpnp.Translations.getString("Local.a0711f44c27e9c05"));
        panelVisionEnabled.add(lblStopAfterWrong, "2, 10, right, default");

        checkBoxStopAfterWrongPart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxStopAfterWrongPart, "4, 10");

        lblFontSizept = new JLabel(org.openpnp.Translations.getString("Local.816b9c6a1631fda1"));
        lblFontSizept.setToolTipText(org.openpnp.Translations.getString("Local.6e877be138045045"));
        panelVisionEnabled.add(lblFontSizept, "8, 10, right, default");

        textFieldFontSizePt = new JTextField();
        panelVisionEnabled.add(textFieldFontSizePt, "10, 10");
        textFieldFontSizePt.setColumns(10);

        btnEditPipeline = new JButton(editPipelineAction);
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        btnSetPartByOcr = new JButton(performOcrAction);
        panelVisionEnabled.add(btnSetPartByOcr, "12, 10, 3, 1");

        lblDiscoverOnJobStart = new JLabel(org.openpnp.Translations.getString("Local.639ba41f313928b9"));
        lblDiscoverOnJobStart.setToolTipText(org.openpnp.Translations.getString("Local.edc77065cf1eb46b"));
        panelVisionEnabled.add(lblDiscoverOnJobStart, "2, 12, right, default");

        checkBoxDiscoverOnJobStart = new JCheckBox("");
        panelVisionEnabled.add(checkBoxDiscoverOnJobStart, "4, 12");

        btnOcrAllFeeders = new JButton(allFeederOcrAction);
        panelVisionEnabled.add(btnOcrAllFeeders, "12, 12, 3, 1");
        panelVisionEnabled.add(btnEditPipeline, "2, 16");

        lblVisionType = new JLabel(org.openpnp.Translations.getString("Local.9d4cddc8f39741f1"));
        lblVisionType.setToolTipText(org.openpnp.Translations.getString("Local.4acdd614c18ab4c9"));
        panelVisionEnabled.add(lblVisionType, "8, 16, right, default");

        pipelineType = new org.openpnp.gui.components.LocalizedComboBox(PipelineType.values());

        panelVisionEnabled.add(pipelineType, "10, 16, fill, default");

        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "12, 16, 3, 1");

        panelCloning = new JPanel();
        panelCloning.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.060d85b818e527b5"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCloning);
        panelCloning.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblUsedAsTemplate = new JLabel(org.openpnp.Translations.getString("Local.6f6d3872756ebd96"));
        panelCloning.add(lblUsedAsTemplate, "2, 2, right, default");
        lblUsedAsTemplate.setToolTipText(org.openpnp.Translations.getString("Local.833dfb835c4cfdc4"));

        checkBoxUsedAsTemplate = new JCheckBox("");
        checkBoxUsedAsTemplate.setToolTipText(org.openpnp.Translations.getString("Local.833dfb835c4cfdc4"));
        checkBoxUsedAsTemplate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnSmartClone != null) {
                    btnSmartClone.setAction(checkBoxUsedAsTemplate.isSelected() ? feederCloneToAllAction: feederCloneFromTemplate);
                }
            }});
        panelCloning.add(checkBoxUsedAsTemplate, "4, 2");

        lblCloneLocationSettings = new JLabel(org.openpnp.Translations.getString("Local.3a5f14d5ac91eebe"));
        lblCloneLocationSettings.setToolTipText(org.openpnp.Translations.getString("Local.6c1551537abf0e25"));
        panelCloning.add(lblCloneLocationSettings, "8, 2, right, default");

        checkBoxCloneLocationSettings = new JCheckBox("");
        checkBoxCloneLocationSettings.setSelected(true);
        checkBoxCloneLocationSettings.setToolTipText(org.openpnp.Translations.getString("Local.6c1551537abf0e25"));
        panelCloning.add(checkBoxCloneLocationSettings, "10, 2");

        btnSmartClone = new JButton(feeder.isUsedAsTemplate() ? feederCloneToAllAction : feederCloneFromTemplate);
        panelCloning.add(btnSmartClone, "14, 2, 1, 7");

        lblTemplate = new JLabel(org.openpnp.Translations.getString("Local.3c8938774eb83dc5"));
        panelCloning.add(lblTemplate, "2, 4, right, default");

        textPaneCloneTemplateStatus = new JTextPane();
        textPaneCloneTemplateStatus.setText("&nbsp;");
        textPaneCloneTemplateStatus.setBackground(UIManager.getColor("control"));
        textPaneCloneTemplateStatus.setContentType("text/html");
        textPaneCloneTemplateStatus.setEditable(false);
        textPaneCloneTemplateStatus.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        JScrollPane textScrollPane = new JScrollPane(textPaneCloneTemplateStatus);
        textScrollPane.setPreferredSize(new Dimension(400, 70));
        panelCloning.add(textScrollPane, "4, 4, 1, 5, default, top");

        lblCloneTapeSetting = new JLabel(org.openpnp.Translations.getString("Local.c457895433c287d9"));
        lblCloneTapeSetting.setToolTipText(org.openpnp.Translations.getString("Local.3cc045ae60b11842"));
        panelCloning.add(lblCloneTapeSetting, "8, 4, right, default");

        checkBoxCloneTapeSettings = new JCheckBox("");
        checkBoxCloneTapeSettings.setSelected(true);
        checkBoxCloneTapeSettings.setToolTipText(org.openpnp.Translations.getString("Local.3cc045ae60b11842"));
        panelCloning.add(checkBoxCloneTapeSettings, "10, 4");

        lblCloneVisionSettings = new JLabel(org.openpnp.Translations.getString("Local.ab3a25d410928601"));
        lblCloneVisionSettings.setToolTipText(org.openpnp.Translations.getString("Local.d5600da14437ae4e"));
        panelCloning.add(lblCloneVisionSettings, "8, 6, right, default");

        checkBoxCloneVisionSettings = new JCheckBox("");
        checkBoxCloneVisionSettings.setToolTipText(org.openpnp.Translations.getString("Local.d5600da14437ae4e"));
        checkBoxCloneVisionSettings.setSelected(true);
        panelCloning.add(checkBoxCloneVisionSettings, "10, 6");

        lblClonePushpullSettings = new JLabel(org.openpnp.Translations.getString("Local.a604e46938c743de"));
        lblClonePushpullSettings.setToolTipText(org.openpnp.Translations.getString("Local.94cb8f7376dcebfd"));
        panelCloning.add(lblClonePushpullSettings, "8, 8, right, default");

        checkBoxClonePushPullSettings = new JCheckBox("");
        checkBoxClonePushPullSettings.setToolTipText(org.openpnp.Translations.getString("Local.94cb8f7376dcebfd"));
        checkBoxClonePushPullSettings.setSelected(true);
        panelCloning.add(checkBoxClonePushPullSettings, "10, 8");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        MutableLocationProxy firstPickLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", firstPickLocation, "location");
        addWrappedBinding(firstPickLocation, "lengthX", textFieldPickLocationX, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthY", textFieldPickLocationY, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthZ", textFieldPickLocationZ, "text",
                lengthConverter);

        addWrappedBinding(feeder, "normalizePickLocation", checkBoxNormalizePickLocation, "selected");

        addWrappedBinding(feeder, "rotationInFeeder", textFieldRotationInTape, "text",
                doubleConverter);

        MutableLocationProxy hole1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole1Location", hole1Location, "location");
        addWrappedBinding(hole1Location, "lengthX", textFieldHole1LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole1Location, "lengthY", textFieldHole1LocationY, "text",
                lengthConverter);

        MutableLocationProxy hole2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole2Location", hole2Location, "location");
        addWrappedBinding(hole2Location, "lengthX", textFieldHole2LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole2Location, "lengthY", textFieldHole2LocationY, "text",
                lengthConverter);

        addWrappedBinding(feeder, "snapToAxis", checkBoxSnapToAxis, "selected");

        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedPitch", textFieldFeedPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedMultiplier", textFieldFeedMultiplier, "text", longConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", longConverter);

        bind(UpdateStrategy.READ_WRITE, feeder, "usedAsTemplate", checkBoxUsedAsTemplate, "selected");

        addWrappedBinding(feeder, "calibrationTrigger", comboBoxCalibrationTrigger, "selectedItem");

        addWrappedBinding(feeder, "precisionWanted", textFieldPrecisionWanted, "text", lengthConverter);
        addWrappedBinding(feeder, "calibrationCount", textFieldCalibrationCount, "text", intConverter);
        addWrappedBinding(feeder, "precisionAverage", textFieldPrecisionAverage, "text", lengthConverter);
        addWrappedBinding(feeder, "precisionConfidenceLimit", textFieldPrecisionConfidenceLimit, "text", lengthConverter);

        addWrappedBinding(feeder, "ocrWrongPartAction", comboBoxWrongPartAction, "selectedItem");
        addWrappedBinding(feeder, "ocrStopAfterWrongPart", checkBoxStopAfterWrongPart, "selected");
        addWrappedBinding(feeder, "ocrDiscoverOnJobStart", checkBoxDiscoverOnJobStart, "selected");
        addWrappedBinding(feeder, "ocrFontName", comboBoxFontName, "selectedItem");
        addWrappedBinding(feeder, "ocrFontSizePt", textFieldFontSizePt, "text", doubleConverter);
        addWrappedBinding(feeder, "pipelineType", pipelineType, "selectedItem");

        addWrappedBinding(feeder, "cloneTemplateStatus", textPaneCloneTemplateStatus, "text");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldRotationInTape);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole2LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedPitch);
        ComponentDecorators.decorateWithAutoSelect(textFieldFontSizePt);
    }

    private Action editPipelineAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.382eac4077ca856c")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.958fee644754aa44"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                UiUtils.confirmMoveToLocationAndAct(
                        getTopLevelAncestor(), 
                        "move the camera to the proper feeder vision location before editing the pipeline", 
                        feeder.getCamera(), 
                        feeder.getNominalVisionLocation(), 
                        true, () -> {
                            editPipeline();
                        });
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.47a6eb4dae7fd9eb")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.927d389f8d75a0f3"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PipelineType type = (PipelineType) pipelineType.getSelectedItem();
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    org.openpnp.Translations.format("Local.566d389915d3b0a7", (type)),
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                applyAction.actionPerformed(null);
                UiUtils.messageBoxOnException(() -> {
                    feeder.resetPipeline(type);
                });
            }
        }
    };

    private Action resetStatisticsAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.fd77d9a2d916d712")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.8e144a774effd908"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetCalibrationStatistics();
            });
        }
    };

    private Action resetFeedCountAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.57d18a01d9cef7ed")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.f29bcda0865e8736"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    org.openpnp.Translations.getString("Local.28a6d609035c443e"),
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    // we apply this because it is OpenPNP custom to do so 
                    applyAction.actionPerformed(e);
                    // set it back to 0
                    feeder.setFeedCount(0);
                });
            }
        }
    };
    private Action discardPartsAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.5585e091bb1a68f9")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.32913f56cdc06785"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                // we apply this because it is OpenPNP custom to do so 
                applyAction.actionPerformed(e);
                feeder.discardParts();
            });
        }
    };
    private Action showVisionFeaturesAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.2d0b0eb1bf5e23bc")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.9016b7ad49d27c06"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.showFeatures();
            });
        }
    };
    private Action autoSetupAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.fa68a054a59cb362"), Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.b997d5276d948394"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result;
                if (!feeder.getLocation().multiply(1, 1, 0, 0).isInitialized()) {
                    // if the feeder.location X, Y is zero, we assume this is a freshly created feeder 
                    result = JOptionPane.YES_OPTION; 
                }
                else {
                    // ask the user
                    result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                            org.openpnp.Translations.format("Local.30358dd02f439c75", ((feeder.isUsedAsTemplate() ? 
                                    "<br/><p color=\"red\">This feeder is marked as template. Are you really, really sure?</p>" : ""))),
                            null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (result == JOptionPane.YES_OPTION) {
                    applyAction.actionPerformed(e);
                    UiUtils.submitUiMachineTask(() -> {
                        feeder.autoSetup();
                    });
                }
            });
        }
    };
    private Action allFeederOcrAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.652477462b1d21b1")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.e0eadac2c4968d09"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                StringBuilder report = new StringBuilder();
                feeder.performOcrOnAllFeeders(null, false, report);
                SwingUtilities.invokeLater(() -> {
                    if (report.length() == 0) {
                        report.append("No action taken.");
                    }
                    JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", org.openpnp.Translations.getString("Local.26ba12a2efe8c82d"), JOptionPane.INFORMATION_MESSAGE);
                });
            });
        }
    };

    private Action setupOcrRegionAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.dc8aa6efaeccd18c")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.b7b5ee661397c673"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(feeder.getCamera(), feeder.getNominalVisionLocation());
                MovableUtils.fireTargetedUserAction(feeder.getCamera());
                SwingUtilities.invokeAndWait(() -> {
                    UiUtils.messageBoxOnException(() -> {
                        new RegionOfInterestProcess(MainFrame.get(), feeder.getCamera(), "Setup OCR Region", true) {
                            @Override 
                            public void setResult(RegionOfInterest roi) {
                                feeder.setOcrRegion(roi);
                            }
                        };
                    });
                });
            });
        }
    };

    private Action performOcrAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.d99348e77efb1652")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.0a4c816ba980b2c3"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(feeder.getCamera(), feeder.getOcrLocation());
                MovableUtils.fireTargetedUserAction(feeder.getCamera());
                StringBuilder report = new StringBuilder();
                feeder.performOcr(OcrWrongPartAction.ChangePart, false, report);
                if (report.length() == 0) {
                    report.append("No action taken.");
                }
                JOptionPane.showMessageDialog(getTopLevelAncestor(), "<html>"+report+"</html>", org.openpnp.Translations.getString("Local.26ba12a2efe8c82d"), JOptionPane.INFORMATION_MESSAGE);
            });
        }
    };

    private Action feederCloneFromTemplate =
            new AbstractAction(org.openpnp.Translations.getString("Local.76099f6eb08b2b88"), Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.000c5cda3fcc0c9b"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception(org.openpnp.Translations.getString("Local.6de3ad44cba4133f"));
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception(org.openpnp.Translations.getString("Local.c18b7d45d0ba78bd"));
                }
                applyAction.actionPerformed(e);
                if (feeder.getTemplateFeeder(null) == null) {
                    throw new Exception(org.openpnp.Translations.getString("Local.23c5c34517beaa7c"));
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        org.openpnp.Translations.format("Local.315a3b6da58283d9", (feeder.getCloneTemplateStatus())),
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    feeder.smartClone(null, 
                            checkBoxCloneLocationSettings.isSelected(),
                            checkBoxCloneTapeSettings.isSelected(), 
                            checkBoxClonePushPullSettings.isSelected(),
                            checkBoxCloneVisionSettings.isSelected(), checkBoxCloneVisionSettings.isSelected());
                }
            });
        }
    };

    private Action feederCloneToAllAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.58ef46c75e08c8ba"), Icons.export) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.f05a2e48d685db35"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                if (!checkBoxUsedAsTemplate.isSelected()) {
                    throw new Exception(org.openpnp.Translations.getString("Local.42d3db637cd217b6"));
                }
                if (!(checkBoxCloneTapeSettings.isSelected()  
                        || checkBoxClonePushPullSettings.isSelected()
                        || checkBoxCloneVisionSettings.isSelected())) {
                    throw new Exception(org.openpnp.Translations.getString("Local.c18b7d45d0ba78bd"));
                }
                applyAction.actionPerformed(e);
                if (feeder.getCompatibleFeeders().size() == 0) {
                    throw new Exception(org.openpnp.Translations.getString("Local.b64cd991ec50f2ee"));
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        org.openpnp.Translations.format("Local.4e56f34c960b5933", (feeder.getCloneTemplateStatus())),
                                null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    for (ReferencePushPullFeeder targetFeeder : feeder.getCompatibleFeeders()) {
                        targetFeeder.cloneFeederSettings( 
                                checkBoxCloneLocationSettings.isSelected(),
                                checkBoxCloneTapeSettings.isSelected(), 
                                checkBoxClonePushPullSettings.isSelected(),
                                checkBoxCloneVisionSettings.isSelected(), checkBoxCloneVisionSettings.isSelected(),
                                feeder);
                    }
                }
            });
        }
    };

    private Action plusOneAction =
            new AbstractAction("", Icons.add) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.fac28f73cd280fe0"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                ReferencePushPullFeeder newFeeder = feeder.createNewInRow();
                UiUtils.submitUiMachineTask(() -> {
                    Camera camera = feeder.getCamera(); 
                    MovableUtils.moveToLocationAtSafeZ(camera, newFeeder.getPickLocation(0, null));
                    MovableUtils.fireTargetedUserAction(camera);
                    newFeeder.autoSetup();
                    SwingUtilities.invokeLater(() -> {
                        Configuration.get().getBus().post(new FeederSelectedEvent(newFeeder, this));
                    });
                });
            });
        }
    };

    private void editPipeline() throws Exception {
        Camera camera = feeder.getCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false, true, true);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getName() + " Pipeline", editor);
        dialog.setVisible(true);
    }

    private JLabel lblPartPitch;
    private JTextField textFieldPartPitch;
    private JTextField textFieldFeedPitch;
    private JLabel lblFeedPitch;
    private JPanel panelLocations;
    private JPanel panelTape;
    private JPanel panelVision;
    private JPanel panelVisionEnabled;
    private LocationButtonsPanel locationButtonsPanelFirstPick;
    private LocationButtonsPanel locationButtonsPanelHole1;
    private LocationButtonsPanel locationButtonsPanelHole2;
    private JLabel lblZ_1;
    private JLabel lblRotation;
    private JLabel lblY_1;
    private JLabel lblX_1;
    private JLabel lblPickLocation;
    private JTextField textFieldPickLocationX;
    private JTextField textFieldPickLocationY;
    private JTextField textFieldPickLocationZ;
    private JTextField textFieldRotationInTape;
    private JLabel lblHole1Location;
    private JTextField textFieldHole1LocationX;
    private JTextField textFieldHole1LocationY;
    private JTextField textFieldHole2LocationX;
    private JTextField textFieldHole2LocationY;
    private JButton btnEditPipeline;
    private JButton btnResetPipeline;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnReset;
    private JButton btnDiscardParts;
    private JTextField textFieldFeedMultiplier;
    private JLabel lblMultiplier;
    private JLabel lblHole2Location;
    private JButton btnShowVisionFeatures;
    private JButton btnAutoSetup;
    private JLabel lblCalibrationTrigger;
    private JComboBox comboBoxCalibrationTrigger;
    private JLabel lblCalibrationCount;
    private JTextField textFieldCalibrationCount;
    private JLabel lblPrecisionAverage;
    private JTextField textFieldPrecisionAverage;
    private JLabel lblPrecisionWanted;
    private JTextField textFieldPrecisionWanted;
    private JButton btnResetStatistics;
    private JLabel lblPrecisionConfidenceLimit;
    private JTextField textFieldPrecisionConfidenceLimit;
    private JLabel lblNormalizePickLocation;
    private JCheckBox checkBoxNormalizePickLocation;
    private JButton btnSmartClone;
    private JLabel lblUsedAsTemplate;
    private JCheckBox checkBoxUsedAsTemplate;
    private JButton btnSetupocrregion;
    private JLabel lblOcrFontName;
    private JComboBox comboBoxFontName;
    private JLabel lblFontSizept;
    private JTextField textFieldFontSizePt;
    private JLabel lblOcrWrongPart;
    private JComboBox comboBoxWrongPartAction;
    private JLabel lblDiscoverOnJobStart;
    private JCheckBox checkBoxDiscoverOnJobStart;
    private JButton btnOcrAllFeeders;
    private JLabel lblStopAfterWrong;
    private JCheckBox checkBoxStopAfterWrongPart;
    private JLabel lblSnapToAxis;
    private JCheckBox checkBoxSnapToAxis;
    private JPanel panelCloning;
    private JLabel lblCloneTapeSetting;
    private JCheckBox checkBoxCloneTapeSettings;
    private JLabel lblCloneVisionSettings;
    private JCheckBox checkBoxCloneVisionSettings;
    private JLabel lblClonePushpullSettings;
    private JCheckBox checkBoxClonePushPullSettings;
    private JTextPane textPaneCloneTemplateStatus;
    private JLabel lblTemplate;
    private JButton button;
    private JLabel lblCloneLocationSettings;
    private JCheckBox checkBoxCloneLocationSettings;
    private JButton btnSetPartByOcr;
    private JComboBox pipelineType;
    private JLabel lblVisionType;
}
