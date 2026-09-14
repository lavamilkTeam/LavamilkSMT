/*
 * Copyright (C) 2019-2021 <mark@makr.zone>
 * based on the ReferenceStripFeederConfigurationWizard 
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
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
public class BlindsFeederArrayConfigurationWizard extends AbstractConfigurationWizard {
    private final BlindsFeeder feeder;

    private JTextField textFieldFiducial1X;
    private JTextField textFieldFiducial1Y;
    private JTextField textFieldFiducial2X;
    private JTextField textFieldFiducial2Y;
    private JTextField textFieldFiducial3X;
    private JTextField textFieldFiducial3Y;
    private JLabel lblNormalize;
    private JCheckBox chckbxNormalize;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFiducial1;
    private LocationButtonsPanel locationButtonsPanelFiducial2;
    private LocationButtonsPanel locationButtonsPanelFiducial3;
    private JLabel lblFiducial3Location;
    private JCheckBox chckbxUseVision;
    private JLabel lblUseVision;

    private JButton btnCalibrateFiducials;
    private JButton btnPipelineToAllFeeders;
    private JButton btnExtractOpenscadModel;

    private JLabel lblOcrAction;
    private JComboBox ocrAction;
    private JLabel lblOcrMargin;
    private JTextField ocrMargin;
    private JLabel lblOcrFontName;
    private JComboBox ocrFontName;
    private JLabel lblFontSizept;
    private JTextField ocrFontSizePt;
    private JLabel lblOcrTextOrientation;
    private JComboBox ocrTextOrientation;
    private JButton btnSetOcrSettings;

    private JPanel panelArray;
    private JLabel label;
    private JLabel lblGroupName;
    private JComboBox feederGroupName;

    public BlindsFeederArrayConfigurationWizard(BlindsFeeder feeder) {
        this.feeder = feeder;
        List<String> blindsFeederGroupNames = feeder.getBlindsFeederGroupNames();

        panelArray = new JPanel();
        contentPanel.add(panelArray);
        panelArray.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.f0cf39d0be3efbb6"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelArray.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(140dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("max(16dlu;min)"),}));

        lblGroupName = new JLabel(org.openpnp.Translations.getString("Local.3e01624a74738d49"));
        panelArray.add(lblGroupName, "2, 2, right, default");
        feederGroupName = new org.openpnp.gui.components.LocalizedComboBox(blindsFeederGroupNames.toArray());
        feederGroupName.setEditable(true);
        panelArray.add(feederGroupName, "4, 2, fill, default");

        label_1 = new JLabel(" ");
        panelArray.add(label_1, "6, 2");

        btnExtractOpenscadModel = new JButton(extract3DPrintingAction);
        panelArray.add(btnExtractOpenscadModel, "8, 2, 1, 3, right, default");


        panelLocations = new JPanel();
        contentPanel.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.95c61170318a2851"), TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default:grow"),},
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


        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 4, center, default");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 4, center, default");

        JLabel lblFiducial1Location = new JLabel(org.openpnp.Translations.getString("Local.7530676e19171ba9"));
        lblFiducial1Location.setToolTipText(
                org.openpnp.Translations.getString("Local.e299f860b97f9dba"));
        panelLocations.add(lblFiducial1Location, "2, 6, right, default");

        textFieldFiducial1X = new JTextField();
        panelLocations.add(textFieldFiducial1X, "4, 6");
        textFieldFiducial1X.setColumns(8);

        textFieldFiducial1Y = new JTextField();
        panelLocations.add(textFieldFiducial1Y, "6, 6");
        textFieldFiducial1Y.setColumns(8);


        locationButtonsPanelFiducial1 = new LocationButtonsPanel(textFieldFiducial1X,
                textFieldFiducial1Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial1, "10, 6");

        JLabel lblFiducial2Location = new JLabel(org.openpnp.Translations.getString("Local.86e9fbdcb6f361ef"));
        lblFiducial2Location.setToolTipText(
                org.openpnp.Translations.getString("Local.ce3fc1c2b424cf51"));
        panelLocations.add(lblFiducial2Location, "2, 8, right, default");

        textFieldFiducial2X = new JTextField();
        panelLocations.add(textFieldFiducial2X, "4, 8");
        textFieldFiducial2X.setColumns(8);

        textFieldFiducial2Y = new JTextField();
        panelLocations.add(textFieldFiducial2Y, "6, 8");
        textFieldFiducial2Y.setColumns(8);


        locationButtonsPanelFiducial2 = new LocationButtonsPanel(textFieldFiducial2X, 
                textFieldFiducial2Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial2, "10, 8");

        lblFiducial3Location = new JLabel(org.openpnp.Translations.getString("Local.52813db5b4a3bbfa"));
        lblFiducial3Location.setToolTipText(org.openpnp.Translations.getString("Local.52043095753b697d"));
        panelLocations.add(lblFiducial3Location, "2, 10, right, default");

        textFieldFiducial3X = new JTextField();
        textFieldFiducial3X.setColumns(8);
        panelLocations.add(textFieldFiducial3X, "4, 10");

        textFieldFiducial3Y = new JTextField();
        textFieldFiducial3Y.setColumns(8);
        panelLocations.add(textFieldFiducial3Y, "6, 10");

        locationButtonsPanelFiducial3 = new LocationButtonsPanel(textFieldFiducial3X, 
                textFieldFiducial3Y, null, null);
        panelLocations.add(locationButtonsPanelFiducial3, "10, 10");

        lblNormalize = new JLabel(org.openpnp.Translations.getString("Local.427ed16be627e7e8"));
        lblNormalize.setToolTipText(org.openpnp.Translations.getString("Local.55a1de1f40fcc794"));
        panelLocations.add(lblNormalize, "2, 12, right, default");

        chckbxNormalize = new JCheckBox("");
        chckbxNormalize.setToolTipText("");
        panelLocations.add(chckbxNormalize, "4, 12");

        btnCalibrateFiducials = new JButton(calibrateFiducialsAction);
        panelLocations.add(btnCalibrateFiducials, "10, 12");

        JPanel panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.88191231d09ac0ea"), TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblUseVision = new JLabel(org.openpnp.Translations.getString("Local.11cbc79c37d76786"));
        lblUseVision.setToolTipText(org.openpnp.Translations.getString("Local.47193cc3231b2d44"));
        panelVision.add(lblUseVision, "2, 2, right, default");

        JButton btnEditPipeline = new JButton(editPipelineAction);

        chckbxUseVision = new JCheckBox("");
        panelVision.add(chckbxUseVision, "4, 2");

        lblOcrAction = new JLabel(org.openpnp.Translations.getString("Local.d81d9855bb1a7f74"));
        panelVision.add(lblOcrAction, "2, 4, right, default");

        ocrAction = new org.openpnp.gui.components.LocalizedComboBox(BlindsFeeder.OcrAction.values());
        ocrAction.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(ocrAction, "4, 4, fill, default");

        List<String> fontList = OcrUtils.createFontSelectionList(feeder.getOcrFontName(), true);

        label = new JLabel(" ");
        panelVision.add(label, "6, 4");

        btnSetOcrSettings = new JButton(setOcrSettingsToAllAction);
        panelVision.add(btnSetOcrSettings, "8, 4");

        lblOcrTextOrientation = new JLabel(org.openpnp.Translations.getString("Local.2267fbb90d5b188c"));
        panelVision.add(lblOcrTextOrientation, "2, 6, right, default");

        ocrTextOrientation = new org.openpnp.gui.components.LocalizedComboBox(BlindsFeeder.OcrTextOrientation.values());
        panelVision.add(ocrTextOrientation, "4, 6, fill, default");

        lblOcrMargin = new JLabel(org.openpnp.Translations.getString("Local.b924ed50cccc58d4"));
        lblOcrMargin.setToolTipText(org.openpnp.Translations.getString("Local.e7ea8d6a7c542180"));
        panelVision.add(lblOcrMargin, "6, 6, right, default");

        ocrMargin = new JTextField();
        panelVision.add(ocrMargin, "8, 6, fill, default");
        ocrMargin.setColumns(10);
        lblOcrFontName = new JLabel(org.openpnp.Translations.getString("Local.72d3c155aed1e770"));
        panelVision.add(lblOcrFontName, "2, 8, right, default");
        ocrFontName = new org.openpnp.gui.components.LocalizedComboBox(fontList.toArray());
        lblOcrFontName.setToolTipText(org.openpnp.Translations.getString("Local.4db4fe2c55e5722e"));
        panelVision.add(ocrFontName, "4, 8, fill, default");

        lblFontSizept = new JLabel(org.openpnp.Translations.getString("Local.acb32fd342453c57"));
        panelVision.add(lblFontSizept, "6, 8, right, default");

        ocrFontSizePt = new JTextField();
        panelVision.add(ocrFontSizePt, "8, 8, fill, default");
        ocrFontSizePt.setColumns(10);

        panelVision.add(btnEditPipeline, "2, 12");

        JButton btnResetPipeline = new JButton(resetPipelineAction);
        panelVision.add(btnResetPipeline, "4, 12");

        btnPipelineToAllFeeders = new JButton(setPipelineToAllAction);
        btnPipelineToAllFeeders.setText(org.openpnp.Translations.getString("Local.f375a4e15013e2f0"));
        panelVision.add(btnPipelineToAllFeeders, "8, 12");

    }

    protected void adaptDialog() {
        // This is arguably counter-intuitive, show the elements always.
        // The code is left in, in case we reconsider.
//        BlindsFeeder.OcrAction action = (OcrAction) ocrAction.getSelectedItem();
//        boolean ocrEnabled = (action != OcrAction.None);
//        lblOcrMargin.setVisible(ocrEnabled);
//        ocrMargin.setVisible(ocrEnabled);
//        lblOcrFontName.setVisible(ocrEnabled);
//        ocrFontName.setVisible(ocrEnabled);
//        lblFontSizept.setVisible(ocrEnabled);
//        ocrFontSizePt.setVisible(ocrEnabled);
//        lblOcrTextOrientation.setVisible(ocrEnabled);
//        ocrTextOrientation.setVisible(ocrEnabled);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        //IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                .getLengthDisplayFormat());

        addWrappedBinding(feeder, "feederGroupName", feederGroupName, "selectedItem");

        MutableLocationProxy location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        MutableLocationProxy fiducial1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial1Location", fiducial1Location, "location");
        addWrappedBinding(fiducial1Location, "lengthX", textFieldFiducial1X, "text", lengthConverter);
        addWrappedBinding(fiducial1Location, "lengthY", textFieldFiducial1Y, "text", lengthConverter);

        MutableLocationProxy fiducial2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial2Location", fiducial2Location, "location");
        addWrappedBinding(fiducial2Location, "lengthX", textFieldFiducial2X, "text", lengthConverter);
        addWrappedBinding(fiducial2Location, "lengthY", textFieldFiducial2Y, "text", lengthConverter);

        MutableLocationProxy fiducial3Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "fiducial3Location", fiducial3Location, "location");
        addWrappedBinding(fiducial3Location, "lengthX", textFieldFiducial3X, "text", lengthConverter);
        addWrappedBinding(fiducial3Location, "lengthY", textFieldFiducial3Y, "text", lengthConverter);

        addWrappedBinding(feeder, "normalize", chckbxNormalize, "selected");

        addWrappedBinding(feeder, "visionEnabled", chckbxUseVision, "selected");
        addWrappedBinding(feeder, "ocrAction", ocrAction, "selectedItem");
        addWrappedBinding(feeder, "ocrMargin", ocrMargin, "text", lengthConverter);
        addWrappedBinding(feeder, "ocrFontName", ocrFontName, "selectedItem");
        addWrappedBinding(feeder, "ocrFontSizePt", ocrFontSizePt, "text", doubleConverter);
        addWrappedBinding(feeder, "ocrTextOrientation", ocrTextOrientation, "selectedItem");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFiducial3Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(ocrMargin);

        adaptDialog();
    }

    private Action extract3DPrintingAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.45549c80df7f5a12"), Icons.openSCadIcon) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.6e37e551d1ff30f8"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                JFileChooser j = new JFileChooser();
                //j.setSelectedFile(directory);
                j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                j.setMultiSelectionEnabled(false);
                if (j.showOpenDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
                    File directory = j.getSelectedFile();
                    boolean opended = true;
                    for (String fileName : new String[] { "BlindsFeeder-Library.scad", "BlindsFeeder-3DPrinting.scad" }) {
                        String fileContent = IOUtils.toString(BlindsFeeder.class
                                .getResource(fileName));
                        File file = new File(directory,  fileName);
                        if (file.exists()) {
                            throw new Exception(org.openpnp.Translations.format("Local.8421d6719ecd63ca", (file.getAbsolutePath())));
                        }
                        try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                            out.print(fileContent);
                        }
                        try {
                            java.awt.Desktop.getDesktop().edit(file);
                        }
                        catch (Exception e1) {
                            Logger.error(e1);
                            opended = false;
                        }
                    }
                    if (! opended) {
                        JOptionPane.showMessageDialog(getTopLevelAncestor(), org.openpnp.Translations.format("Local.b35763d77d2e8f03", (directory.getAbsolutePath())));
                    }
                }
            });
        }
    };

    private Action calibrateFiducialsAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.992077f12f47bd04")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.b3ab943f01c0e4fa"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            calibrateFiducials();
        }
    };

    private Action editPipelineAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.382eac4077ca856c")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.958fee644754aa44"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editPipeline();
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.47a6eb4dae7fd9eb")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.d1a1189cad31e157"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                resetPipeline();
            });
        }
    };

    private Action setOcrSettingsToAllAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.f57cd3c31fe4e7a8")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.49c3c22d85ac0bf4"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        org.openpnp.Translations.getString("Local.c4836fc9f65677a7"),
                        null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        setOcrSettingsToAllFeeders();
                    });
                }
            });
        }
    };

    private Action setPipelineToAllAction =
            new AbstractAction(org.openpnp.Translations.getString("Local.f375a4e15013e2f0")) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    org.openpnp.Translations.getString("Local.0b3b73d7d377c511"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        org.openpnp.Translations.getString("Local.51501d7bd66b69db"),
                        null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        setPipelineToAllFeeders();
                    });
                }
            });
        }
    };
    private JLabel label_1;

    private void calibrateFiducials() {
        UiUtils.submitUiMachineTask(() -> {
            feeder.calibrateFeederLocations();
        });
    }

    private void editPipeline() throws Exception {
        // Make sure we're editing a new common pipeline instance for all the feeders in this array.
        // The setPipeline() will make sure that all the feeders in the array have the same new clone referenced. 
        feeder.setPipeline(feeder.getPipeline().clone());
        // Prepare and edit the pipeline.
        Camera camera = feeder.getCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false, feeder.getOcrAction());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getName() + " Pipeline", editor);
        dialog.setVisible(true);
    }

    private void resetPipeline() {
        feeder.resetPipeline();
    }

    private void setOcrSettingsToAllFeeders() throws CloneNotSupportedException {
        feeder.setOcrSettingsToAllFeeders();
    }

    private void setPipelineToAllFeeders() throws CloneNotSupportedException {
        feeder.setPipelineToAllFeeders();
    }
}

