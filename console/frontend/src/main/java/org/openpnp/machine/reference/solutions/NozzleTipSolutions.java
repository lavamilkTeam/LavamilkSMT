/*
 * Copyright (C) 2022 <mark@makr.zone>
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

package org.openpnp.machine.reference.solutions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.RecalibrationTrigger;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipCalibrationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;

public class NozzleTipSolutions implements Solutions.Subject  {
    private ReferenceMachine machine;

    public NozzleTipSolutions() {
        super();
    }

    public NozzleTipSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Kinematics)) {
            for (Head head : machine.getHeads()) {
                for (Nozzle n : head.getNozzles()) {
                    if (n instanceof ReferenceNozzle) {
                        ReferenceNozzle nozzle = (ReferenceNozzle) n;
                        if (!nozzle.getManualNozzleTipChangeLocation().isInitialized()) {
                            solutions.add(new Solutions.Issue(
                                    nozzle, 
                                    org.openpnp.Translations.message("Local.c0714f7305a07f65", (nozzle.getName())), 
                                    org.openpnp.Translations.message("Local.c3b2925773dde1a7", (nozzle.getName())), 
                                    Solutions.Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#capture-safe-z") {

                                @Override 
                                public void activate() throws Exception {
                                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                                }

                                @Override
                                public Icon getExtendedIcon() {
                                    return Icons.nozzleTipLoad;
                                }

                                @Override 
                                public String getExtendedDescription() {
                                    return org.openpnp.Translations.format("Local.82b210136548b044", (nozzle.getName()));
                                }

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    nozzle.setManualNozzleTipChangeLocation(state == State.Solved ? 
                                            nozzle.getLocation() : Location.origin);
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }
            }
        }
        if (solutions.isTargeting(Milestone.Calibration)) {
            for (NozzleTip nt : machine.getNozzleTips()) {
                if (nt instanceof ReferenceNozzleTip && !((ReferenceNozzleTip) nt).isUnloadedNozzleTipStandin()) {
                    ReferenceNozzleTip nozzleTip = (ReferenceNozzleTip) nt;
                    try {
                        Camera camera = VisionUtils.getBottomVisionCamera();
                        Nozzle defaultNozzle = nozzleTip.getNozzleWhereLoaded();
                        if (defaultNozzle == null) {
                            for (Head head : machine.getHeads()) {
                                for (Nozzle nozzle : head.getNozzles()) {
                                    if (nozzle.getCompatibleNozzleTips().contains(nozzleTip)) {
                                        defaultNozzle = nozzle;
                                        break;
                                    }
                                }
                            }
                        }
                        if (defaultNozzle == null) {
                            solutions.add(new Solutions.PlainIssue(
                                    nozzleTip, 
                                    org.openpnp.Translations.message("Local.f525be6dc998d1b2", (nozzleTip.getName())), 
                                    org.openpnp.Translations.message("Local.21786cbb0680e069"), 
                                    Solutions.Severity.Error,
                                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-to-nozzle-tip-compatibility"));
                        }
                        else {
                            perNozzleTip(solutions, nozzleTip, camera, defaultNozzle);
                        }
                    }
                    catch (Exception e) {
                        Logger.trace(e);
                    }
                }
            }

        }
    }

    protected void perNozzleTip(Solutions solutions, ReferenceNozzleTip nozzleTip, Camera camera,
            Nozzle nozzle) {
        final Length oldVisionDiameter = nozzleTip.getCalibration().getCalibrationTipDiameter();
        final RecalibrationTrigger oldRecalibrationTrigger = nozzleTip.getCalibration().getRecalibrationTrigger();
        final boolean oldFailHoming = nozzleTip.getCalibration().isFailHoming();
        final BackgroundCalibrationMethod oldBackgroundCalibrationMethod = nozzleTip.getCalibration().getBackgroundCalibrationMethod();
        final CvPipeline oldPipeline = nozzleTip.getCalibration().getPipeline(); 
        LengthConverter lengthConverter = new LengthConverter(); 

        if (!nozzleTip.getCalibration().isEnabled()) {
            solutions.add(machine.getVisionSolutions().new VisionFeatureIssue(
                    nozzleTip,
                    (ReferenceCamera) camera,
                    oldVisionDiameter,
                    org.openpnp.Translations.message("Local.19d42eb687d73d38", (nozzleTip.getName())), 
                    org.openpnp.Translations.message("Local.74c9269981aeb620", (nozzleTip.getName())), 
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Calibration-Setup") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                }

                @Override 
                public String getExtendedDescription() {
                    return org.openpnp.Translations.format("Local.b7a6c72c52f6a392", ((nozzleTip != nozzle.getNozzleTip() ?
                                    org.openpnp.Translations.getString("Local.ca2ce30804c5c2a8")+nozzleTip.getName()+org.openpnp.Translations.getString("Local.bf9f25b3af4799a2")+nozzle.getName() + ".</p><br/>"
                                    : org.openpnp.Translations.getString("Local.39468d535fb00eb1")+nozzleTip.getName()+org.openpnp.Translations.getString("Local.e0c54867a5f60daa")+nozzle.getName() + ".</p><br/>")), (nozzle.getName()), (camera.getName()));
                }

                @Override
                public Solutions.Issue.CustomProperty[] getProperties() {
                    Solutions.Issue.CustomProperty[] props1 = super.getProperties();
                    Solutions.Issue.CustomProperty[] props0 = new Solutions.Issue.CustomProperty[] {
                            nozzleTipLoadActionProperty(this, nozzle, nozzleTip),
                            new Solutions.Issue.ActionProperty( 
                                    "", org.openpnp.Translations.format("Local.668bc0573068119b", (nozzle.getName()), (camera.getName()))) {
                                @Override
                                public Action get() {
                                    return new AbstractAction(org.openpnp.Translations.getString("Local.47a68fdc2f30be8a"), Icons.centerTool) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                if (nozzleTip != nozzle.getNozzleTip()) {
                                                    throw new Exception(org.openpnp.Translations.format("Local.1a88f7e8d244bfac", (nozzleTip.getName()), (nozzle.getName())));
                                                }
                                                MovableUtils.moveToLocationAtSafeZ(nozzle, camera.getLocation(nozzle));
                                                MovableUtils.fireTargetedUserAction(nozzle);
                                            });
                                        }
                                    };
                                }
                            },
                    };
                    return Collect.concat(props0, props1);
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        if (nozzleTip != nozzle.getNozzleTip()) {
                            throw new Exception(org.openpnp.Translations.format("Local.1a88f7e8d244bfac", (nozzleTip.getName()), (nozzle.getName())));
                        }
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary camera calibration. 
                                    Length visionDiameter = camera.getUnitsPerPixel().getLengthX().multiply(featureDiameter);
                                    nozzleTip.getCalibration().setCalibrationTipDiameter(visionDiameter);
                                    Logger.info("Set nozzle tip "+nozzleTip.getName()+" vision diameter to "+visionDiameter+" (previously "+oldVisionDiameter+")");
                                    nozzleTip.getCalibration().setEnabled(true);
                                    nozzleTip.getCalibration().resetPipeline();
                                    nozzleTip.getCalibration().setRecalibrationTrigger(RecalibrationTrigger.MachineHome);
                                    nozzleTip.getCalibration().setFailHoming(false);
                                    nozzleTip.getCalibration().calibrate((ReferenceNozzle) nozzle);
                                    return true;
                                },
                                (result) -> {
                                    UiUtils.messageBoxOnException(() -> super.setState(state));
                                },
                                (t) -> {
                                    UiUtils.showError(t);
                                    // restore old state
                                    UiUtils.messageBoxOnException(() -> setState(oldState));
                                });
                    }
                    else {
                        // Restore the old vision diameter.
                        nozzleTip.getCalibration().setCalibrationTipDiameter(oldVisionDiameter);
                        nozzleTip.getCalibration().setEnabled(false);
                        nozzleTip.getCalibration().setPipeline(oldPipeline);
                        nozzleTip.getCalibration().setFailHoming(oldFailHoming);
                        nozzleTip.getCalibration().setRecalibrationTrigger(oldRecalibrationTrigger);
                        super.setState(state);
                    }
                }
            });
        }
        else { 
            if (nozzleTip.getMaxPickTolerance().compareTo(new Length(1.0, LengthUnit.Millimeters)) > 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        org.openpnp.Translations.message("Local.fff865a1c989d9e8", (nozzleTip.getName()), (lengthConverter.convertForward(nozzleTip.getMaxPickTolerance()))),
                        org.openpnp.Translations.message("Local.e7fbdc5963139a0f"),
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            else if (nozzleTip.getMinPartDiameter().compareTo(nozzleTip.getMaxPickTolerance().multiply(2)) <= 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        org.openpnp.Translations.message("Local.b45d1cff00b57635", (nozzleTip.getName()), (lengthConverter.convertForward(nozzleTip.getMinPartDiameter()))),
                        org.openpnp.Translations.message("Local.3d9883766498da78", (lengthConverter.convertForward(nozzleTip.getMaxPickTolerance()))),
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            else if (nozzleTip.getMinPartDiameter().compareTo(nozzleTip.getMaxPartDiameter()) >= 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        org.openpnp.Translations.message("Local.e685ca44bf728ffc", (nozzleTip.getName())),
                        org.openpnp.Translations.message("Local.69148958fc14845e"),
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            if (oldBackgroundCalibrationMethod == BackgroundCalibrationMethod.None) {
                solutions.add(new Solutions.Issue(
                        nozzleTip, 
                        org.openpnp.Translations.message("Local.8aa9d11c1ca02486", (nozzleTip.getName())), 
                        org.openpnp.Translations.message("Local.2ee79a1be6b47555"), 
                        Solutions.Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Background-Calibration") {

                    @Override 
                    public void activate() throws Exception {
                        MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return org.openpnp.Translations.format("Local.bb040ee10ca136d6", (nozzle.getName()), (camera.getName()), ((getState() == State.Solved ? 
                                        org.openpnp.Translations.getString("Local.4a7ce1948f6d95a2")
                                        + "<p style=\"max-width: 40em\">"+nozzleTip.getCalibration().getBackgroundDiagnostics()
                                        .replace("<html>", "").replace("</html>", "").replace("<hr/>", "<br/>")+org.openpnp.Translations.getString("Local.351210cb900cb689")+nozzleTip.getName()+".</p>"
                                        : "")));
                    }

                    @Override
                    public Solutions.Issue.CustomProperty[] getProperties() {
                        return new Solutions.Issue.CustomProperty[] {
                                nozzleTipLoadActionProperty(this, nozzle, nozzleTip),
                        };
                    }

                    @Override
                    public Solutions.Issue.Choice[] getChoices() {
                        return new Solutions.Issue.Choice[] {
                                new Solutions.Issue.Choice(BackgroundCalibrationMethod.BrightnessAndKeyColor, 
                                        org.openpnp.Translations.format("Local.50812ec34ec511e0"),
                                                null),
                                new Solutions.Issue.Choice(BackgroundCalibrationMethod.Brightness, 
                                        org.openpnp.Translations.format("Local.e9faac2021c9ef1b"),
                                                null),
                        };
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        nozzleTip.getCalibration().setBackgroundCalibrationMethod(
                                state == State.Solved ? 
                                        (BackgroundCalibrationMethod) getChoice() : oldBackgroundCalibrationMethod);
                        if (state == State.Solved) {
                            if (nozzleTip != nozzle.getNozzleTip()) {
                                throw new Exception(org.openpnp.Translations.format("Local.1a88f7e8d244bfac", (nozzleTip.getName()), (nozzle.getName())));
                            }
                            UiUtils.submitUiMachineTask(() -> {
                                nozzleTip.getCalibration().calibrate((ReferenceNozzle) nozzle);
                                UiUtils.messageBoxOnExceptionLater(() -> {
                                    super.setState(state);
                                    ReferenceNozzleTipCalibrationWizard.showBackgroundProblems(nozzleTip, false);
                                });
                            });
                        }
                        else {
                            super.setState(state);
                        }
                    }
                });
            }
        }
    }

    protected Solutions.Issue.ActionProperty nozzleTipLoadActionProperty(Solutions.Issue issue, Nozzle nozzle,
            ReferenceNozzleTip nozzleTip) {
        return issue.new ActionProperty( 
                "", org.openpnp.Translations.format("Local.5728c191618a48f3", (nozzleTip.getName()), (nozzle.getName()))) {
            @Override
            public Action get() {
                return new AbstractAction(org.openpnp.Translations.format("Local.73589fed4576a087", (nozzleTip.getName())), Icons.nozzleTipLoad) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UiUtils.submitUiMachineTask(() -> {
                            if (nozzleTip == nozzle.getNozzleTip()) {
                                throw new Exception(org.openpnp.Translations.format("Local.40665c2173d8b486", (nozzleTip.getName()), (nozzle.getName())));
                            }
                            nozzle.loadNozzleTip(nozzleTip);
                        });
                    }
                };
            }
        };
    }

}
