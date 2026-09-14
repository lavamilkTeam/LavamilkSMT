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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.model.Solutions.Subject;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Nozzle.RotationMode;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractNozzle;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class AxisSolutions implements Solutions.Subject {
    public static final String[] VALID_AXIS_LETTERS = new String[] { "X", "Y", "Z", "U", "V", "W", "A", "B", "C", "D", "E" };

    private final ReferenceControllerAxis axis;
    private Machine machine;

    public AxisSolutions(ReferenceControllerAxis axis) {
        this.axis = axis;
        this.machine = Configuration.get().getMachine();
    }

    @Override
    public void findIssues(Solutions solutions) {

        if (solutions.isTargeting(Milestone.Basics)) {
            if (axis.getDriver() == null) {
                solutions.add(new Solutions.PlainIssue(
                        axis, 
                        org.openpnp.Translations.message("Local.f8cbf75763834846"), 
                        org.openpnp.Translations.message("Local.a0b4d5d3e5a46e55"), 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));

            }
            if (axis.getLetter().isEmpty()) {
                solutions.add(new AxisLetterIssue(
                        axis, 
                        org.openpnp.Translations.message("Local.7bbf3c465b9b8972"), 
                        org.openpnp.Translations.message("Local.01cf8c712c0a4da9"), 
                                Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
            }
            else if (axis.getLetter().equals("E")) {
                if (axis.getDriver() != null && !axis.getDriver().isSupportingPreMove()) {
                    solutions.add(new AxisLetterIssue(
                            axis, 
                            org.openpnp.Translations.message("Local.9bfd9892d09d97d9"), 
                            org.openpnp.Translations.message("Local.639e9d8f238e671b"), 
                            Severity.Warning,
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#upgrading-and-configuring-firmwares"));
                }
            }
            else if (!getValidAxisLetters().contains(axis.getLetter())) {
                solutions.add(new AxisLetterIssue(
                        axis, 
                        org.openpnp.Translations.message("Local.e62b7f47ad23e45e", (axis.getLetter()), ((getReportedAxisLetters() != null ? org.openpnp.Translations.message("Local.8c51289332115a65") : org.openpnp.Translations.message("Local.eaab4a1b0d6c3038"))), (String.join(" ", getValidAxisLetters()))), 
                        org.openpnp.Translations.message("Local.39c004db192fdf44", ((getReportedAxisLetters() != null ? org.openpnp.Translations.message("Local.715aa672be04e63d") : org.openpnp.Translations.message("Local.595d095016a42091")))), 
                                Severity.Warning,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
            }
            if (axis.getDriver() != null && !axis.getLetter().isEmpty() 
                    && !(axis.getLetter().equals("E") && axis.getDriver().isSupportingPreMove())) {
                ArrayList<String> duplicates = new ArrayList<>();
                for (Axis otherAxis : machine.getAxes()) {
                    if (otherAxis instanceof AbstractControllerAxis) {
                        AbstractControllerAxis otherControllerAxis = (AbstractControllerAxis) otherAxis;
                        if (otherControllerAxis.getDriver() == axis.getDriver()
                                && otherControllerAxis.getLetter().equals(axis.getLetter())) {
                            duplicates.add(otherControllerAxis.getName());
                        }
                    }
                }
                if (duplicates.size() > 1) {
                    solutions.add(new AxisLetterIssue(
                            axis, 
                            org.openpnp.Translations.message("Local.2f28d503f3b7e3c8", (axis.getLetter()), (String.join(", ", duplicates))), 
                            org.openpnp.Translations.message("Local.fa6e1a04679fe9db"), 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
                }
            }
        }
        if (solutions.isTargeting(Milestone.Kinematics)) {
            if (axis.getMotionLimit(1) > 0 
                && Math.abs(axis.getMotionLimit(1)*2 - axis.getMotionLimit(2)) < 0.1) {
                // HACK: migration sets the acceleration to twice the feed-rate, that's our "signal" that the user has not yet
                // tuned them.
                solutions.add(new Solutions.PlainIssue(
                        axis, 
                        org.openpnp.Translations.message("Local.8ca30f123142e385"), 
                        org.openpnp.Translations.message("Local.45b430b048fb9446", (axis.getClass().getSimpleName()), (axis.getName())), 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
            }
            if (axis.getDriver() != null) {
                MotionControlType motionControlType = axis.getDriver().getMotionControlType();
                if (motionControlType.isControllingFeedRate()
                        && axis.getMotionLimit(1) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            org.openpnp.Translations.message("Local.27d37aee2fe02ff2", (motionControlType), (axis.getName())), 
                            org.openpnp.Translations.message("Local.8565bef74581dfa9", (axis.getClass().getSimpleName()), (axis.getName())), 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
                if (motionControlType.isControllingAcceleration()
                        && axis.getMotionLimit(2) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            org.openpnp.Translations.message("Local.895128e557469319", (motionControlType), (axis.getName())), 
                            org.openpnp.Translations.message("Local.dbc60dfd86d7c509", (axis.getClass().getSimpleName()), (axis.getName())), 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
                if (motionControlType.isControllingJerk()
                        && axis.getMotionLimit(3) <= 0) {
                    solutions.add(new Solutions.PlainIssue(
                            axis, 
                            org.openpnp.Translations.message("Local.6e77d357b3266d17", (motionControlType), (axis.getName())), 
                            org.openpnp.Translations.message("Local.1b212e81872d4cda", (axis.getClass().getSimpleName()), (axis.getName())), 
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
                }
            }
            if (axis.getType() == Type.Rotation) {
                boolean isUnlimitedArticulation = true;
                if (axis.getDefaultHeadMountable() instanceof AbstractNozzle) {
                    AbstractNozzle nozzle = (AbstractNozzle)axis.getDefaultHeadMountable();
                    double[] limits = new double[] {-180, 180};
                    try {
                        limits = nozzle.getRotationModeLimits();
                    }
                    catch (Exception e) {
                        // ignore this here
                    }
                    final double epsilon = 1e-5;
                    if (nozzle.getRotationMode() == RotationMode.LimitedArticulation) {
                        isUnlimitedArticulation = false;
                    }
                    else if (limits[1] - limits[0] < 360 - epsilon) {
                        isUnlimitedArticulation = false;
                        final RotationMode oldRotationMode = nozzle.getRotationMode();
                        solutions.add(new Solutions.Issue(
                                nozzle, 
                                org.openpnp.Translations.message("Local.5abe8c85fdcec385", (axis.getName()), (nozzle.getName()), (RotationMode.LimitedArticulation)), 
                                org.openpnp.Translations.message("Local.8e781d2b845e9e4c", (RotationMode.LimitedArticulation)), 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                nozzle.setRotationMode((state == Solutions.State.Solved) ?
                                        RotationMode.LimitedArticulation :
                                            oldRotationMode);
                                super.setState(state);
                            }
                        });
                    }
                    if (!nozzle.isAligningRotationMode()) {
                        solutions.add(new Solutions.Issue(
                                nozzle, 
                                org.openpnp.Translations.message("Local.e28f68bfbdf55038", (nozzle.getName())), 
                                        org.openpnp.Translations.message("Local.1bbc10100ff5e5dd"), 
                                        Severity.Suggestion,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode#align-nozzle-rotation-with-part") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                nozzle.setAligningRotationMode((state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }
                    if (!isUnlimitedArticulation) {
                        // Checking that ReferenceBottomVision has pre-rotate enabled. 
                        for (PartAlignment partAlignment : machine.getPartAlignments()) {
                            if (partAlignment instanceof ReferenceBottomVision) {
                                ReferenceBottomVision referenceBottomVision = (ReferenceBottomVision) partAlignment;
                                if (!referenceBottomVision.isPreRotate()) {
                                    solutions.add(new Solutions.Issue(
                                            referenceBottomVision, 
                                            org.openpnp.Translations.message("Local.239221f9075d0e1f"), 
                                            org.openpnp.Translations.message("Local.86bcc45a59a37d27"), 
                                            Severity.Error,
                                            "https://github.com/openpnp/openpnp/wiki/Bottom-Vision#global-configuration") {

                                        @Override
                                        public void setState(Solutions.State state) throws Exception {
                                            referenceBottomVision.setPreRotate((state == Solutions.State.Solved));
                                            super.setState(state);
                                        }
                                    });
                                }
                                // Check all parts.
                                List<BottomVisionSettings> visionSettings = new ArrayList<>();
                                List<String> items = new ArrayList<>();
                                for (AbstractVisionSettings settings : Configuration.get().getVisionSettings()) {
                                    if (settings instanceof BottomVisionSettings
                                            && ((BottomVisionSettings) settings).getPreRotateUsage() == PreRotateUsage.AlwaysOff) {
                                        items.add(settings.getName());
                                        visionSettings.add((BottomVisionSettings) settings);
                                    }
                                }
                                items.sort(null);
                                if (!visionSettings.isEmpty()) {
                                    solutions.add(new Solutions.Issue(
                                            referenceBottomVision, 
                                            org.openpnp.Translations.message("Local.cb679d3d542bf591"), 
                                                    org.openpnp.Translations.message("Local.0f19c5a008bd0956", (PreRotateUsage.AlwaysOff), (PreRotateUsage.Default)), 
                                                    Severity.Error,
                                            "https://github.com/openpnp/openpnp/wiki/Bottom-Vision#part-configuration") {


                                        @Override 
                                        public String getExtendedDescription() {
                                            return org.openpnp.Translations.format("Local.ecfeb97eae35c88e", (PreRotateUsage.AlwaysOff), (PreRotateUsage.Default), (items.stream().collect(Collectors.joining("</li><li>"))));
                                        }

                                        @Override
                                        public void setState(Solutions.State state) throws Exception {
                                            for (BottomVisionSettings setting : visionSettings) {
                                                setting.setPreRotateUsage((state == State.Solved) ?
                                                        PreRotateUsage.Default : PreRotateUsage.AlwaysOff);
                                            }
                                            super.setState(state);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                if (isUnlimitedArticulation) {
                    if (axis.getDefaultHeadMountable() instanceof Nozzle) {
                        // Axis is used on nozzle, suggest some optimizations.
                        if (!axis.isWrapAroundRotation()) {
                            solutions.add(new Solutions.Issue(
                                    axis, 
                                    org.openpnp.Translations.message("Local.0e86e7b90ab132f2"), 
                                    org.openpnp.Translations.message("Local.5e09050470e24667"), 
                                    Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    axis.setWrapAroundRotation((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                        if (!axis.isLimitRotation()) {
                            solutions.add(new Solutions.Issue(
                                    axis, 
                                    org.openpnp.Translations.message("Local.4e003e1c8e577a81"), 
                                            org.openpnp.Translations.message("Local.7f481f7094211af2"), 
                                            Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    axis.setLimitRotation((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }
                else {
                    // Limited articulation, we need to treat things differently.
                    if (axis.isWrapAroundRotation()) {
                        solutions.add(new Solutions.Issue(
                                axis, 
                                org.openpnp.Translations.message("Local.d851fdbecafde864"), 
                                org.openpnp.Translations.message("Local.f997c1ff9b3348ee"), 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode#setting-up-the-nozzle-rotation-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                axis.setWrapAroundRotation(!(state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }
                    if (!axis.isLimitRotation()) {
                        solutions.add(new Solutions.Issue(
                                axis, 
                                org.openpnp.Translations.message("Local.7795360cf8c170de"), 
                                org.openpnp.Translations.message("Local.7f481f7094211af2"), 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Nozzle-Rotation-Mode#setting-up-the-nozzle-rotation-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                axis.setLimitRotation((state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }
                }
            }
        }
    }

    protected List<String> getReportedAxisLetters() {
        if (axis.getDriver() instanceof GcodeDriver)  { 
            List<String> letters = ((GcodeDriver)axis.getDriver()).getReportedAxesLetters();
            if (letters.size() > 0) {
                return letters;
            }
        }
        return null;
    }

    protected List<String> getValidAxisLetters() {
        List<String> reportedAxisLetters = getReportedAxisLetters();
        if (reportedAxisLetters != null) {
            return reportedAxisLetters;
        }
        return Arrays.asList(AxisSolutions.VALID_AXIS_LETTERS);
    }

    public class AxisLetterIssue extends Solutions.Issue {
        final String oldAxisLetter;
        String newAxisLetter;
        public AxisLetterIssue(Subject subject, Object issue, Object solution, Severity severity, String uri) {
            super(subject, issue, solution, severity, uri);
            oldAxisLetter = axis.getLetter();
            if (oldAxisLetter.isEmpty()) {
                String suggestedAxisLetter = axis.getName().toUpperCase().substring(0, 1);
                if ("XYZ".contains(suggestedAxisLetter)) {
                    newAxisLetter = suggestedAxisLetter;
                }
            }
            else {
                newAxisLetter = oldAxisLetter;
            }
        }

        @Override
        public void setState(Solutions.State state) throws Exception {
            if (state == State.Solved) {
                if (newAxisLetter == null || newAxisLetter.isEmpty()) {
                    throw new Exception(org.openpnp.Translations.getString("Local.6c1fedb592c44814"));
                }
                axis.setLetter(newAxisLetter);
            }
            else {
                axis.setLetter(oldAxisLetter);
            }
            super.setState(state);
        }

        @Override
        public Solutions.Issue.CustomProperty[] getProperties() {
            return new Solutions.Issue.CustomProperty[] {
                    new Solutions.Issue.StringProperty(
                            org.openpnp.Translations.format("Local.6650ea31a30b7999"),
                            org.openpnp.Translations.format("Local.6884ab034fdce484")) {

                        @Override
                        public String get() {
                            return newAxisLetter;
                        }

                        @Override
                        public void set(String value) {
                            newAxisLetter = value;
                        }

                        @Override
                        public String[] getSuggestions() {
                            ArrayList<String> list = new ArrayList<>();
                            for (String axisLetter : getValidAxisLetters()) {
                                boolean taken = false;
                                for (Axis otherAxis : machine.getAxes()) {
                                    if (otherAxis != axis && otherAxis instanceof AbstractControllerAxis) {
                                        AbstractControllerAxis otherControllerAxis = (AbstractControllerAxis) otherAxis;
                                        if (otherControllerAxis.getDriver() == axis.getDriver()
                                                && otherControllerAxis.getLetter().equals(axisLetter)) {
                                            taken = true;
                                            break;
                                        }
                                    }
                                }
                                if (!taken) {
                                    list.add(axisLetter);
                                }
                            }
                            return list.toArray(new String[list.size()]);
                        }
                    }
            };
        }
    }
}
