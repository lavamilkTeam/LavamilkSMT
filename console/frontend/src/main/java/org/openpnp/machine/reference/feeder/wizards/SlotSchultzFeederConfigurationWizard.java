/*
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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder.Bank;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder.Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SlotSchultzFeederConfigurationWizard
extends AbstractConfigurationWizard {
    private final SlotSchultzFeeder feeder;

    private JComboBox comboBoxFeedActuator;
    private JTextField actuatorValue;

    private JComboBox comboBoxPostPickActuator;

    private JButton btnTestFeedActuator;
    private JButton btnTestPostPickActuator;

    private JComboBox comboBoxFeedCountActuator;
    private JButton btnGetFeedCountActuator;
    private JTextField feedCountValue;

    private JComboBox comboBoxClearCountActuator;
    private JButton btnClearCountActuator;

    private JComboBox comboBoxPitchActuator;
    private JButton btnPitchActuator;
    private JTextField pitchValue;

    private JComboBox comboBoxTogglePitchActuator;
    private JButton btnTogglePitchActuator;

    private JComboBox comboBoxStatusActuator;
    private JButton btnStatusActuator;
    private JTextField statusText;

    private JComboBox comboBoxIdActuator;
    private JButton btnIdActuator;
    private JTextField idText;

    private JTextField feederNameTf;
    private JTextField bankNameTf;

    private JComboBox feederCb;
    private JComboBox bankCb;    
    private JComboBox feederPartCb;
    private JTextField fiducialPartTf;
    private JTextField xOffsetTf;
    private JTextField yOffsetTf;
    private JTextField zOffsetTf;
    private JTextField rotOffsetTf;
    private JTextField xPickLocTf;
    private JTextField yPickLocTf;
    private JTextField zPickLocTf;
    private JTextField rotPickLocTf;
    private LocationButtonsPanel offsetLocButtons;
    private LocationButtonsPanel pickLocButtons;
    private JTextField feedRetryCount;
    private JTextField pickRetryCount;

    public SlotSchultzFeederConfigurationWizard(SlotSchultzFeeder feeder) {
        this.feeder = feeder;

        JPanel slotPanel = new JPanel();
        slotPanel.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.79689782061dec13"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(slotPanel);
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));

        JPanel whateverPanel = new JPanel();
        slotPanel.add(whateverPanel);
        FormLayout fl_whateverPanel = new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,});
        fl_whateverPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        whateverPanel.setLayout(fl_whateverPanel);

        feederNameTf = new JTextField();
        whateverPanel.add(feederNameTf, "8, 2, 3, 1");
        feederNameTf.setColumns(10);

        JPanel panel_1 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel_1, "12, 2");

        JButton loadFeederBtn = new JButton(loadFeederAction);
        loadFeederBtn.setToolTipText(org.openpnp.Translations.getString("Local.2c38c6bf14de64d4"));
        panel_1.add(loadFeederBtn);

        //        JButton newFeederBtn = new JButton(newFeederAction);
        //        panel_1.add(newFeederBtn);

        JButton deleteFeederBtn = new JButton(deleteFeederAction);
        deleteFeederBtn.setToolTipText(org.openpnp.Translations.getString("Local.ff291f6d9bf8f9f8"));
        panel_1.add(deleteFeederBtn);

        JLabel lblPickRetryCount = new JLabel(org.openpnp.Translations.getString("Local.8ddba9c217ccfc71"));
        whateverPanel.add(lblPickRetryCount, "2, 12, right, default");

        pickRetryCount = new JTextField();
        pickRetryCount.setColumns(10);
        whateverPanel.add(pickRetryCount, "4, 12, fill, default");

        JLabel lblBank = new JLabel(org.openpnp.Translations.getString("Local.676c471bc8dc3d13"));
        whateverPanel.add(lblBank, "2, 14, right, default");

        bankCb = new org.openpnp.gui.components.LocalizedComboBox();
        whateverPanel.add(bankCb, "4, 14, 3, 1");
        bankCb.addActionListener(e -> {
            feederCb.removeAllItems();
            Bank bank = (Bank) bankCb.getSelectedItem();
            feederCb.addItem(null);
            if (bank != null) {
                for (Feeder f : bank.getFeeders()) {
                    feederCb.addItem(f);
                }
            }
        });

        JLabel lblFeeder = new JLabel(org.openpnp.Translations.getString("Local.1d575865e20e7899"));
        whateverPanel.add(lblFeeder, "2, 2, right, default");

        feederCb = new org.openpnp.gui.components.LocalizedComboBox();
        whateverPanel.add(feederCb, "4, 2, 3, 1");

        JPanel feederPanel = new JPanel();
        feederPanel.setBorder(new TitledBorder(null, org.openpnp.Translations.getString("Local.1d575865e20e7899"), TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(feederPanel);
        FormLayout fl_feederPanel = new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,});
        fl_feederPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        feederPanel.setLayout(fl_feederPanel);

        JLabel lblX_1 = new JLabel("X");
        feederPanel.add(lblX_1, "4, 2");

        JLabel lblY_1 = new JLabel("Y");
        feederPanel.add(lblY_1, "6, 2");

        JLabel lblZ_1 = new JLabel("Z");
        feederPanel.add(lblZ_1, "8, 2");

        JLabel lblRotation_1 = new JLabel(org.openpnp.Translations.getString("Local.57b5e2fc1bba18bb"));
        feederPanel.add(lblRotation_1, "10, 2");

        JLabel lblOffsets = new JLabel(org.openpnp.Translations.getString("Local.8807635f8ee82973"));
        feederPanel.add(lblOffsets, "2, 4");

        xOffsetTf = new JTextField();
        feederPanel.add(xOffsetTf, "4, 4");
        xOffsetTf.setColumns(10);

        yOffsetTf = new JTextField();
        feederPanel.add(yOffsetTf, "6, 4");
        yOffsetTf.setColumns(10);

        zOffsetTf = new JTextField();
        feederPanel.add(zOffsetTf, "8, 4");
        zOffsetTf.setColumns(10);

        rotOffsetTf = new JTextField();
        feederPanel.add(rotOffsetTf, "10, 4");
        rotOffsetTf.setColumns(10);

        //        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, rotOffsetTf);
        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, null);
        feederPanel.add(offsetLocButtons, "12, 4");

        JLabel lblPart = new JLabel(org.openpnp.Translations.getString("Local.8570a2e1669ff057"));
        feederPanel.add(lblPart, "2, 6, right, default");

        feederPartCb = new org.openpnp.gui.components.LocalizedComboBox();
        feederPanel.add(feederPartCb, "4, 6, 3, 1");
        feederPartCb.setModel(new PartsComboBoxModel());
        feederPartCb.setRenderer(new IdentifiableListCellRenderer<Part>());

        JPanel panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(null,
                org.openpnp.Translations.getString("Local.90087245130446b5"), TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelActuator);
        panelActuator.setLayout(new FormLayout(new ColumnSpec[] {
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

        JLabel lblActuatorValue = new JLabel(org.openpnp.Translations.getString("Local.d32cf14c21c1ae69"));
        panelActuator.add(lblActuatorValue, "4, 2, right, default");

        actuatorValue = new JTextField();
        panelActuator.add(actuatorValue, "6, 2");
        actuatorValue.setColumns(6);

        JLabel lblActuator = new JLabel(org.openpnp.Translations.getString("Local.7ceb75dce547812c"));
        panelActuator.add(lblActuator, "4, 4, left, default");

        JLabel lblGetID = new JLabel(org.openpnp.Translations.getString("Local.a0432959f0926e6b"));
        panelActuator.add(lblGetID, "2, 6, right, default");

        comboBoxIdActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxIdActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxIdActuator, "4, 6, fill, default");

        btnIdActuator = new JButton(getIdActuatorAction);
        panelActuator.add(btnIdActuator, "6, 6");

        idText = new JTextField();
        idText.setColumns(10);
        panelActuator.add(idText, "8, 6");

        JLabel lblFeed = new JLabel(org.openpnp.Translations.getString("Local.888e757afd2a83d4"));
        panelActuator.add(lblFeed, "2, 8, right, default");

        comboBoxFeedActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedActuator, "4, 8, fill, default");

        btnTestFeedActuator = new JButton(testFeedActuatorAction);
        panelActuator.add(btnTestFeedActuator, "6, 8");

        JLabel lblPostPick = new JLabel(org.openpnp.Translations.getString("Local.0ec1c7006cf078d2"));
        panelActuator.add(lblPostPick, "2, 10, right, default");

        comboBoxPostPickActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxPostPickActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPostPickActuator, "4, 10, fill, default");

        btnTestPostPickActuator = new JButton(testPostPickActuatorAction);
        panelActuator.add(btnTestPostPickActuator, "6, 10");

        JLabel lblFeedCount = new JLabel(org.openpnp.Translations.getString("Local.879afef5c1f90d9d"));
        panelActuator.add(lblFeedCount, "2, 12, right, default");

        comboBoxFeedCountActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxFeedCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedCountActuator, "4, 12, fill, default");

        btnGetFeedCountActuator = new JButton(getFeedCountActuatorAction);
        panelActuator.add(btnGetFeedCountActuator, "6, 12");

        feedCountValue = new JTextField();
        feedCountValue.setColumns(8);
        panelActuator.add(feedCountValue, "8, 12");

        JLabel lblClearCount = new JLabel(org.openpnp.Translations.getString("Local.30c552dfbedc7ee3"));
        panelActuator.add(lblClearCount, "2, 14, right, default");

        comboBoxClearCountActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxClearCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxClearCountActuator, "4, 14, fill, default");

        btnClearCountActuator = new JButton(clearCountActuatorAction);
        panelActuator.add(btnClearCountActuator, "6, 14");

        JLabel lblGetPitch = new JLabel(org.openpnp.Translations.getString("Local.b8a3616c85a8c5af"));
        panelActuator.add(lblGetPitch, "2, 16, right, default");

        comboBoxPitchActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxPitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPitchActuator, "4, 16, fill, default");

        btnPitchActuator = new JButton(pitchActuatorAction);
        panelActuator.add(btnPitchActuator, "6, 16");

        pitchValue = new JTextField();
        pitchValue.setColumns(8);
        panelActuator.add(pitchValue, "8, 16");

        JLabel lblTogglePitch = new JLabel(org.openpnp.Translations.getString("Local.83684e99a60fcaa3"));
        panelActuator.add(lblTogglePitch, "2, 18, right, default");

        comboBoxTogglePitchActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxTogglePitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxTogglePitchActuator, "4, 18, fill, default");

        btnTogglePitchActuator = new JButton(togglePitchActuatorAction);
        panelActuator.add(btnTogglePitchActuator, "6, 18");

        JLabel lblTogglePitchDesc = new JLabel(org.openpnp.Translations.getString("Local.36a4fe517bc0cf0b"));
        panelActuator.add(lblTogglePitchDesc, "8, 18, left, default");

        JLabel lblGetStatus = new JLabel(org.openpnp.Translations.getString("Local.9785937391e6d39b"));
        panelActuator.add(lblGetStatus, "2, 20, right, default");

        comboBoxStatusActuator = new org.openpnp.gui.components.LocalizedComboBox();
        comboBoxStatusActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxStatusActuator, "4, 20, fill, default");

        btnStatusActuator = new JButton(statusActuatorAction);
        panelActuator.add(btnStatusActuator, "6, 20");

        statusText = new JTextField();
        statusText.setColumns(50);
        panelActuator.add(statusText, "8, 20");

        if(Configuration.get().getMachine().isEnabled()){
            getIdActuatorAction.actionPerformed(null);
            getFeedCountActuatorAction.actionPerformed(null);
            pitchActuatorAction.actionPerformed(null);
            statusActuatorAction.actionPerformed(null);
        }

        for (Bank bank : SlotSchultzFeeder.getBanks()) {
            bankCb.addItem(bank);
        }
        feederCb.addItem(null);

        JLabel lblX = new JLabel("X");
        whateverPanel.add(lblX, "4, 4, center, default");

        JLabel lblY = new JLabel("Y");
        whateverPanel.add(lblY, "6, 4, center, default");

        JLabel lblZ = new JLabel("Z");
        whateverPanel.add(lblZ, "8, 4, center, default");

        JLabel lblRotation = new JLabel(org.openpnp.Translations.getString("Local.57b5e2fc1bba18bb"));
        whateverPanel.add(lblRotation, "10, 4, center, default");

        JLabel lblPickLocation = new JLabel(org.openpnp.Translations.getString("Local.15b61974b2707a7b"));
        whateverPanel.add(lblPickLocation, "2, 6, right, default");

        xPickLocTf = new JTextField();
        whateverPanel.add(xPickLocTf, "4, 6");
        xPickLocTf.setColumns(10);

        yPickLocTf = new JTextField();
        whateverPanel.add(yPickLocTf, "6, 6");
        yPickLocTf.setColumns(10);

        zPickLocTf = new JTextField();
        whateverPanel.add(zPickLocTf, "8, 6");
        zPickLocTf.setColumns(10);

        pickLocButtons = new LocationButtonsPanel(xPickLocTf, yPickLocTf, zPickLocTf, rotPickLocTf);

        rotPickLocTf = new JTextField();
        whateverPanel.add(rotPickLocTf, "10, 6");
        rotPickLocTf.setColumns(10);
        whateverPanel.add(pickLocButtons, "12, 6");

        JButton fiducialAlign = new JButton(updateLocationAction);
        whateverPanel.add(fiducialAlign, "14, 6");
        fiducialAlign.setIcon(Icons.fiducialCheck);
        fiducialAlign.setToolTipText(org.openpnp.Translations.getString("Local.95aac95a8971a40c"));

        JLabel lblFiducialPart = new JLabel(org.openpnp.Translations.getString("Local.311961aa263799e9"));
        whateverPanel.add(lblFiducialPart, "2, 8, right, default");

        fiducialPartTf = new JTextField();
        whateverPanel.add(fiducialPartTf, "4, 8, 3, 1");
        fiducialPartTf.addActionListener(e -> {
            feeder.setFiducialPart(fiducialPartTf.getText());
        });

        JLabel lblFeedRetryCount = new JLabel(org.openpnp.Translations.getString("Local.9f59e0267cd9701f"));
        whateverPanel.add(lblFeedRetryCount, "2, 10, right, default");

        feedRetryCount = new JTextField();
        whateverPanel.add(feedRetryCount, "4, 10");
        feedRetryCount.setColumns(10);

        bankNameTf = new JTextField();
        whateverPanel.add(bankNameTf, "8, 14, 3, 1");
        bankNameTf.setColumns(10);

        JPanel panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel, "12, 14");

        JButton newBankBtn = new JButton(newBankAction);
        panel.add(newBankBtn);

        JButton deleteBankBtn = new JButton(deleteBankAction);
        panel.add(deleteBankBtn);
        if (feeder.getBank() != null) {
            for (Feeder f : feeder.getBank().getFeeders()) {
                feederCb.addItem(f);
            }
        }
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "fiducialPart", fiducialPartTf, "text");
        addWrappedBinding(feeder, "feedRetryCount", feedRetryCount, "text", intConverter);
        addWrappedBinding(feeder, "pickRetryCount", pickRetryCount, "text", intConverter);

        addWrappedBinding(feeder, "actuatorName", comboBoxFeedActuator, "selectedItem");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);

        addWrappedBinding(feeder, "postPickActuatorName", comboBoxPostPickActuator, "selectedItem");

        addWrappedBinding(feeder, "feedCountActuatorName", comboBoxFeedCountActuator, "selectedItem");

        addWrappedBinding(feeder, "clearCountActuatorName", comboBoxClearCountActuator, "selectedItem");

        addWrappedBinding(feeder, "pitchActuatorName", comboBoxPitchActuator, "selectedItem");

        addWrappedBinding(feeder, "togglePitchActuatorName", comboBoxTogglePitchActuator, "selectedItem");

        addWrappedBinding(feeder, "statusActuatorName", comboBoxStatusActuator, "selectedItem");

        addWrappedBinding(feeder, "idActuatorName", comboBoxIdActuator, "selectedItem");

        /**
         * Note that we set up the bindings here differently than everywhere else. In most
         * wizards the fields are bound with wrapped bindings and the proxy is bound with a hard
         * binding. Here we do the opposite so that when the user captures a new location
         * it is set on the proxy immediately. This allows the offsets to update immediately.
         * I'm not actually sure why we do it the other way everywhere else, since this seems
         * to work fine. Might not matter in most other cases. 
         */
        MutableLocationProxy pickLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "location", pickLocation, "location");
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthX", xPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthY", yPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthZ", zPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "rotation", rotPickLocTf, "text", doubleConverter);
        bind(UpdateStrategy.READ, pickLocation, "location", offsetLocButtons, "baseLocation");

        /**
         * The strategy for the bank and feeder properties are a little complex:
         * We create an observable wrapper for bank and feeder. We add wrapped bindings
         * for these against the source feeder, so if they are changed, then upon hitting
         * Apply they will be changed on the source.
         * In addition we add non-wrapped bindings from the bank and feeder wrappers to their
         * instance properties such as name and part. Thus they will be updated immediately.
         */
        Wrapper<Bank> bankWrapper = new Wrapper<>();
        Wrapper<Feeder> feederWrapper = new Wrapper<>();

        addWrappedBinding(feeder, "bank", bankWrapper, "value");
        addWrappedBinding(feeder, "feeder", feederWrapper, "value");

        bind(UpdateStrategy.READ_WRITE, bankWrapper, "value", bankCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, bankWrapper, "value.name", bankNameTf, "text")
        .addBindingListener(new AbstractBindingListener() {
            @Override
            public void synced(Binding binding) {
                SwingUtilities.invokeLater(() -> bankCb.repaint());
            }
        });
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value", feederCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.name", feederNameTf, "text")
        .addBindingListener(new AbstractBindingListener() {
            @Override
            public void synced(Binding binding) {
                SwingUtilities.invokeLater(() -> feederCb.repaint());
            }
        });
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.part", feederPartCb, "selectedItem");

        MutableLocationProxy offsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.offsets", offsets, "location");
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthX", xOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthY", yOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthZ", zOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "rotation", rotOffsetTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(feederNameTf);
        ComponentDecorators.decorateWithAutoSelect(bankNameTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zPickLocTf);
        ComponentDecorators.decorateWithAutoSelect(rotPickLocTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zOffsetTf);
        ComponentDecorators.decorateWithAutoSelect(rotOffsetTf);

        ComponentDecorators.decorateWithAutoSelect(fiducialPartTf);

        ComponentDecorators.decorateWithAutoSelect(feedRetryCount);
        ComponentDecorators.decorateWithAutoSelect(pickRetryCount);

        feederPartCb.addActionListener(e -> {
            notifyChange();
        });

    }

    private Action loadFeederAction = new AbstractAction(org.openpnp.Translations.getString("Local.8a6bdb6b18da586f")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder(idText.getText());
            Feeder item;
            int i;
            for (i = 1; i < feederCb.getItemCount(); i++) {
                item = (Feeder) feederCb.getItemAt(i);
                if (item.getName().equals(f.getName()))  {
                    feederCb.setSelectedIndex(i);
                    break;
                }
            }
            if (i == feederCb.getItemCount()) {	  // list did not contain feeder, so create it
                Logger.warn("No feeder {} exists in bank, so creating new.", f);
                bank.getFeeders().add(f);
                feederCb.addItem(f);
                feederCb.setSelectedItem(f);
                xOffsetTf.setText("-5");		// set default offsets for new feeder
                yOffsetTf.setText("-30");
            }
        }
    };

    /*    private Action newFeederAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder(idText.getText());
            bank.getFeeders().add(f);
            feederCb.addItem(f);
            feederCb.setSelectedItem(f);
        	xOffsetTf.setText("-5");		// set default offsets for new feeder
        	yOffsetTf.setText("-30");
        }
    };
     */

    private Action deleteFeederAction = new AbstractAction(org.openpnp.Translations.getString("Local.e2d0a54968ead24e")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            Feeder feeder = (Feeder) feederCb.getSelectedItem();
            Bank bank = (Bank) bankCb.getSelectedItem();
            bank.getFeeders().remove(feeder);
            feederCb.removeItem(feeder);
        }
    };

    private Action newBankAction = new AbstractAction(org.openpnp.Translations.getString("Local.18fdd549b2ed367a")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = new Bank();
            SlotSchultzFeeder.getBanks().add(bank);
            bankCb.addItem(bank);
            bankCb.setSelectedItem(bank);
        }
    };

    private Action deleteBankAction = new AbstractAction(org.openpnp.Translations.getString("Local.e2d0a54968ead24e")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (SlotSchultzFeeder.getBanks().size() < 2) {
                MessageBoxes.errorBox(getTopLevelAncestor(), org.openpnp.Translations.getString("Local.54a0e8c17ebb21a1"), org.openpnp.Translations.getString("Local.52bdfbc1890bfca1"));
                return;
            }
            SlotSchultzFeeder.getBanks().remove(bank);
            bankCb.removeItem(bank);
        }
    };

    private Action getIdActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.a0432959f0926e6b")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getIdActuatorName() == null || feeder.getIdActuatorName().equals("")) {
                    Logger.warn("No getIdActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getIdActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getIdActuatorName())));
                }
                String s = actuator.read(feeder.getActuatorValue());
                SwingUtilities.invokeLater(() -> {
                    idText.setText(s == null ? "" : s);
                });
            });
        }
    };

    private Action testFeedActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.0e2eb1ab15abe2eb")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getActuatorName() == null || feeder.getActuatorName().equals("")) {
                    Logger.warn("No actuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getActuatorName());

                if (actuator == null) {
                    throw new Exception(org.openpnp.Translations.format("Local.cb7dc96a5d0da56d", (feeder.getActuatorName())));
                }
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
            });
        }
    };

    private Action testPostPickActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.c38b031e83bab0a8")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getPostPickActuatorName() == null || feeder.getPostPickActuatorName().equals("")) {
                    Logger.warn("No postPickActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getPostPickActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cb7dc96a5d0da56d", (feeder.getPostPickActuatorName())));
                }
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
            });
        }
    };

    private Action getFeedCountActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.e6cfc9a762836c22")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getFeedCountActuatorName() == null || feeder.getFeedCountActuatorName().equals("")) {
                    Logger.warn("No feedCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getFeedCountActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getFeedCountActuatorName())));
                }
                String s = actuator.read(feeder.getActuatorValue());
                SwingUtilities.invokeLater(() -> {
                    feedCountValue.setText(s == null ? "" : s);
                });
            });
        }
    };

    private Action clearCountActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.3a739048629268cd")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getClearCountActuatorName() == null || feeder.getClearCountActuatorName().equals("")) {
                    Logger.warn("No clearCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getClearCountActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getClearCountActuatorName())));
                }
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
                SwingUtilities.invokeLater(() -> {
                    feedCountValue.setText("");
                });
            });
        }
    };

    private Action pitchActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.f0ae6b359334ff8a")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getPitchActuatorName() == null || feeder.getPitchActuatorName().equals("")) {
                    Logger.warn("No feedCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getPitchActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getPitchActuatorName())));
                }
                String s = actuator.read(feeder.getActuatorValue());
                SwingUtilities.invokeLater(() -> {
                    pitchValue.setText(s == null ? "" : s);
                });
            });
        }
    };

    private Action togglePitchActuatorAction = new AbstractAction(org.openpnp.Translations.getString("Local.c654bb4ffa4b88aa")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getTogglePitchActuatorName() == null || feeder.getTogglePitchActuatorName().equals("")) {
                    Logger.warn("No togglePitchActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getTogglePitchActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getTogglePitchActuatorName())));
                }
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
                pitchActuatorAction.actionPerformed(null);
            });
        }
    };

    private Action statusActuatorAction =  new AbstractAction(org.openpnp.Translations.getString("Local.21eea2b9e8e8f513")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getStatusActuatorName() == null || feeder.getStatusActuatorName().equals("")) {
                    Logger.warn("No statusActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getStatusActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            org.openpnp.Translations.format("Local.cad39535ca0cfb82", (feeder.getStatusActuatorName())));
                }
                String s = actuator.read(feeder.getActuatorValue());
                SwingUtilities.invokeLater(() -> {
                    statusText.setText(s == null ? "" : s);
                });
            });
        }
    };

    private Action updateLocationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (feeder.getFiducialPart() == null) {
                    Logger.warn("No fiducial defined for feeder {}.", feeder.getName());
                    return;
                }
                Location newLocation = feeder.getFiducialLocation(feeder.getLocation(), feeder.getFiducialPart());
                if (newLocation == null) {
                    throw new Exception(org.openpnp.Translations.getString("Local.4360bd9dd7c39c5c"));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        xPickLocTf.setText(newLocation.getLengthX().toString());
                        yPickLocTf.setText(newLocation.getLengthY().toString());
                    });
                }
            });
        }
    };
}
