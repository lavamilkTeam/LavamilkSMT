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

package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView.RenderingQuality;
import org.openpnp.gui.components.CameraView.ZoomSensitivity;
import org.openpnp.gui.components.reticle.CrosshairReticle;
import org.openpnp.gui.components.reticle.FiducialReticle;
import org.openpnp.gui.components.reticle.GridReticle;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.components.reticle.RulerReticle;
import org.openpnp.gui.processes.EstimateObjectZCoordinateProcess;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

// TODO: For the time being, since setting a property on the reticle doesn't re-save it we are
// making a redundant call to setReticle on every property update. Fix that somehow.
@SuppressWarnings("serial")
public class CameraViewPopupMenu extends JPopupMenu {
    private CameraView cameraView;
    private JMenu zoomIncMenu;
    private JMenu reticleMenu;
    private JMenu reticleOptionsMenu;
    private JMenu renderingQualityMenu;

    public CameraViewPopupMenu(CameraView cameraView) {
        this.cameraView = cameraView;

        // For cameras that have been calibrated at two different heights, add menu options to reset
        // the viewing plane and for estimating an object's height
        if (cameraView.isViewingPlaneChangable()) {
            JMenuItem mntmEstimateZCoordinate = new JMenuItem(org.openpnp.Translations.getString("Local.7be59c1f7c090829"));
            mntmEstimateZCoordinate.addActionListener(estimateZCoordinateAction);
            add(mntmEstimateZCoordinate);
        }

        // For non-movable cameras, add a menu option to move the selected nozzle to the camera
        if (cameraView.getCamera().getHead() == null) {
            JMenuItem mntmMoveSelectedNozzleToCamera = new JMenuItem(org.openpnp.Translations.getString("Local.fe4262c9049873dc"));
            mntmMoveSelectedNozzleToCamera.addActionListener(moveSelectedNozzleToCameraAction);
            add(mntmMoveSelectedNozzleToCamera);
        }

        zoomIncMenu = createZoomIncMenu();

        add(zoomIncMenu);

        renderingQualityMenu = createRenderingQualityMenu();

        add(renderingQualityMenu);

        reticleMenu = createReticleMenu();

        add(reticleMenu);

        JCheckBoxMenuItem chkShowImageInfo = new JCheckBoxMenuItem(showImageInfoAction);
        chkShowImageInfo.setSelected(cameraView.isShowImageInfo());
        add(chkShowImageInfo);


        if (cameraView.getDefaultReticle() != null) {
            if (cameraView.getDefaultReticle() instanceof RulerReticle) {
                setReticleOptionsMenu(createRulerReticleOptionsMenu(
                        (RulerReticle) cameraView.getDefaultReticle()));
            }
            else if (cameraView.getDefaultReticle() instanceof GridReticle) {
                setReticleOptionsMenu(createRulerReticleOptionsMenu(
                        (GridReticle) cameraView.getDefaultReticle()));
            }
            else if (cameraView.getDefaultReticle() instanceof FiducialReticle) {
                setReticleOptionsMenu(createFiducialReticleOptionsMenu(
                        (FiducialReticle) cameraView.getDefaultReticle()));
            }
            else if (cameraView.getDefaultReticle() instanceof CrosshairReticle) {
                setReticleOptionsMenu(createCrosshairReticleOptionsMenu(
                        (CrosshairReticle) cameraView.getDefaultReticle()));
            }
        }
    }

    private JMenu createZoomIncMenu() {
        JMenu subMenu = new JMenu(org.openpnp.Translations.getString("Local.04fe28cfbcc3b3cc"));
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.c4ebc6d4a5832cd9"));
        buttonGroup.add(menuItem);
        if (cameraView.getZoomIncPerMouseWheelTick()
                == CameraView.zoomIncrements.get(ZoomSensitivity.High)) {
            menuItem.setSelected(true);
        }
        menuItem.setToolTipText(org.openpnp.Translations.getString("Local.7f56c596d9e45415"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setZoomIncPerMouseWheelTick(
                        CameraView.zoomIncrements.get(ZoomSensitivity.High));
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.8e588cd187741f1c"));
        buttonGroup.add(menuItem);
        if (cameraView.getZoomIncPerMouseWheelTick()
                == CameraView.zoomIncrements.get(ZoomSensitivity.Medium)) {
            menuItem.setSelected(true);
        }
        menuItem.setToolTipText(org.openpnp.Translations.getString("Local.f41e448d89093200"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setZoomIncPerMouseWheelTick(
                        CameraView.zoomIncrements.get(ZoomSensitivity.Medium));
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.f793de205ead5ac3"));
        buttonGroup.add(menuItem);
        if (cameraView.getZoomIncPerMouseWheelTick()
                == CameraView.zoomIncrements.get(ZoomSensitivity.Low)) {
            menuItem.setSelected(true);
        }
        menuItem.setToolTipText(org.openpnp.Translations.getString("Local.43bcf2aefda0aa23"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setZoomIncPerMouseWheelTick(
                        CameraView.zoomIncrements.get(ZoomSensitivity.Low));
            }
        });
        subMenu.add(menuItem);
        
        return subMenu;
    }

    private JMenu createRenderingQualityMenu() {
        JMenu subMenu = new JMenu(org.openpnp.Translations.getString("Local.fb222e6355b234a4"));
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem menuItem;
        
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.86ff9c8de6516bd5"));
        buttonGroup.add(menuItem);
        if (cameraView.getRenderingQuality() == RenderingQuality.Low) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setRenderingQuality(RenderingQuality.Low);
            }
        });
        subMenu.add(menuItem);
        
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.b030ef3ea2ab50e1"));
        buttonGroup.add(menuItem);
        if (cameraView.getRenderingQuality() == RenderingQuality.High) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setRenderingQuality(RenderingQuality.High);
            }
        });
        subMenu.add(menuItem);
        
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.c84d3ef7aa7e5053"));
        buttonGroup.add(menuItem);
        if (cameraView.getRenderingQuality() == RenderingQuality.BestScale) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraView.setRenderingQuality(RenderingQuality.BestScale);
            }
        });
        subMenu.add(menuItem);
        
        return subMenu;
    }

    private JMenu createReticleMenu() {
        JMenu menu = new JMenu(org.openpnp.Translations.getString("Local.45679780a3c25c00"));

        ButtonGroup buttonGroup = new ButtonGroup();

        JRadioButtonMenuItem menuItem;

        Reticle reticle = cameraView.getDefaultReticle();

        menuItem = new JRadioButtonMenuItem(noReticleAction);
        if (reticle == null) {
            menuItem.setSelected(true);
        }
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem(crosshairReticleAction);
        if (reticle != null && reticle.getClass() == CrosshairReticle.class) {
            menuItem.setSelected(true);
        }
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem(gridReticleAction);
        if (reticle != null && reticle.getClass() == GridReticle.class) {
            menuItem.setSelected(true);
        }
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem(rulerReticleAction);
        if (reticle != null && reticle.getClass() == RulerReticle.class) {
            menuItem.setSelected(true);
        }
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem(fiducialReticleAction);
        if (reticle != null && reticle.getClass() == FiducialReticle.class) {
            menuItem.setSelected(true);
        }
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        return menu;
    }
    
    private JMenuItem createColorMenuItem(String name, Color color, ButtonGroup buttonGroup, CrosshairReticle reticle) {
        JMenuItem menuItem = new JRadioButtonMenuItem(name);
        buttonGroup.add(menuItem);
        if (reticle.getColor().equals(color)) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setColor(color);
                cameraView.setDefaultReticle(reticle);
            }
        });
        return menuItem;
    }

    private JMenu createCrosshairReticleOptionsMenu(final CrosshairReticle reticle) {
        JMenu menu = new JMenu(org.openpnp.Translations.getString("Local.d0db8b5e364b6989"));

        ButtonGroup buttonGroup = new ButtonGroup();

        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ba19e9c3d5f49882"), Color.red, buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.d486dfbd5fb57834"), Color.green, buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.19dd83f117525b93"), Color.yellow, buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.78e7771b8b46e11d"), Color.decode("#ffd35d"), buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ec7d56a01607001e"), Color.blue, buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.3495e757855a5c67"), Color.white, buttonGroup, reticle));
        menu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ba19e9c3d5f49882"), Color.red, buttonGroup, reticle));

        return menu;
    }

    private JMenu createRulerReticleOptionsMenu(final RulerReticle reticle) {
        JMenu menu = new JMenu(org.openpnp.Translations.getString("Local.d0db8b5e364b6989"));

        JMenu subMenu;
        JRadioButtonMenuItem menuItem;
        ButtonGroup buttonGroup;

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.6b73191a0a4b6742"));
        buttonGroup = new ButtonGroup();
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ba19e9c3d5f49882"), Color.red, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.d486dfbd5fb57834"), Color.green, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.19dd83f117525b93"), Color.yellow, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.78e7771b8b46e11d"), Color.decode("#ffd35d"), buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ec7d56a01607001e"), Color.blue, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.3495e757855a5c67"), Color.white, buttonGroup, reticle));
        menu.add(subMenu);

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.9fb6669a77ea48bb"));
        buttonGroup = new ButtonGroup();
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.f222baf08e531ee9"));
        buttonGroup.add(menuItem);
        if (reticle.getUnits() == LengthUnit.Millimeters) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnits(LengthUnit.Millimeters);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.9eb5a39899c34c45"));
        buttonGroup.add(menuItem);
        if (reticle.getUnits() == LengthUnit.Inches) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnits(LengthUnit.Inches);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menu.add(subMenu);

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.d0e9030a733bba3b"));
        buttonGroup = new ButtonGroup();
        menuItem = new JRadioButtonMenuItem("0.1");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 0.1) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(0.1);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("0.25");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 0.25) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(0.25);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("0.50");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 0.50) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(0.50);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("1");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 1) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(1);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("2");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 2) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(2);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("5");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 5) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(5);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("10");
        buttonGroup.add(menuItem);
        if (reticle.getUnitsPerTick() == 10) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnitsPerTick(10);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menu.add(subMenu);

        return menu;
    }

    private JMenu createFiducialReticleOptionsMenu(final FiducialReticle reticle) {
        JMenu menu = new JMenu(org.openpnp.Translations.getString("Local.d0db8b5e364b6989"));

        JMenu subMenu;
        JRadioButtonMenuItem menuItem;
        ButtonGroup buttonGroup;

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.6b73191a0a4b6742"));
        buttonGroup = new ButtonGroup();
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ba19e9c3d5f49882"), Color.red, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.d486dfbd5fb57834"), Color.green, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.19dd83f117525b93"), Color.yellow, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.78e7771b8b46e11d"), Color.decode("#ffd35d"), buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.ec7d56a01607001e"), Color.blue, buttonGroup, reticle));
        subMenu.add(createColorMenuItem(org.openpnp.Translations.format("Local.3495e757855a5c67"), Color.white, buttonGroup, reticle));
        menu.add(subMenu);

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.9fb6669a77ea48bb"));
        buttonGroup = new ButtonGroup();
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.f222baf08e531ee9"));
        buttonGroup.add(menuItem);
        if (reticle.getUnits() == LengthUnit.Millimeters) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnits(LengthUnit.Millimeters);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.9eb5a39899c34c45"));
        buttonGroup.add(menuItem);
        if (reticle.getUnits() == LengthUnit.Inches) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setUnits(LengthUnit.Inches);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menu.add(subMenu);

        subMenu = new JMenu(org.openpnp.Translations.getString("Local.e0e492707f85ff1f"));
        buttonGroup = new ButtonGroup();
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.b93d3bcecff5d749"));
        buttonGroup.add(menuItem);
        if (reticle.getShape() == FiducialReticle.Shape.Circle) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setShape(FiducialReticle.Shape.Circle);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem(org.openpnp.Translations.format("Local.c11092bc0861591e"));
        buttonGroup.add(menuItem);
        if (reticle.getShape() == FiducialReticle.Shape.Square) {
            menuItem.setSelected(true);
        }
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setShape(FiducialReticle.Shape.Square);
                cameraView.setDefaultReticle(reticle);
            }
        });
        subMenu.add(menuItem);
        menu.add(subMenu);

        JCheckBoxMenuItem chkMenuItem = new JCheckBoxMenuItem(org.openpnp.Translations.format("Local.3a1bc53ffcdf2473"));
        chkMenuItem.setSelected(reticle.isFilled());
        chkMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reticle.setFilled(((JCheckBoxMenuItem) e.getSource()).isSelected());
                cameraView.setDefaultReticle(reticle);
            }
        });
        menu.add(chkMenuItem);

        JMenuItem inputMenuItem = new JMenuItem(org.openpnp.Translations.getString("Local.1af851907331c0ed"));
        inputMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String result = JOptionPane.showInputDialog(cameraView,
                        String.format(org.openpnp.Translations.getString("Local.52cfd3e34b4749a8"),
                                reticle.getUnits().toString().toLowerCase()),
                        reticle.getSize() + "");
                if (result != null) {
                    reticle.setSize(Double.valueOf(result));
                    cameraView.setDefaultReticle(reticle);
                }
            }
        });
        menu.add(inputMenuItem);

        return menu;
    }

    private void setReticleOptionsMenu(JMenu menu) {
        if (reticleOptionsMenu != null) {
            reticleMenu.remove(reticleMenu.getMenuComponentCount() - 1);
            reticleMenu.remove(reticleMenu.getMenuComponentCount() - 1);
        }
        if (menu != null) {
            reticleMenu.addSeparator();
            reticleMenu.add(menu);
        }
        reticleOptionsMenu = menu;
    }

    private Action showImageInfoAction = new AbstractAction(org.openpnp.Translations.getString("Local.6e1ec7a010558f53")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            cameraView.setShowImageInfo(((JCheckBoxMenuItem) e.getSource()).isSelected());
        }
    };

    private Action noReticleAction = new AbstractAction(org.openpnp.Translations.getString("Local.dc937b59892604f5")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            setReticleOptionsMenu(null);
            cameraView.setDefaultReticle(null);
        }
    };

    private Action crosshairReticleAction = new AbstractAction(org.openpnp.Translations.getString("Local.8b3214b0cef93026")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            CrosshairReticle reticle = new CrosshairReticle();
            JMenu optionsMenu = createCrosshairReticleOptionsMenu(reticle);
            setReticleOptionsMenu(optionsMenu);
            cameraView.setDefaultReticle(reticle);
        }
    };

    private Action gridReticleAction = new AbstractAction(org.openpnp.Translations.getString("Local.0d7d12ac624c66e6")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            GridReticle reticle = new GridReticle();
            JMenu optionsMenu = createRulerReticleOptionsMenu(reticle);
            setReticleOptionsMenu(optionsMenu);
            cameraView.setDefaultReticle(reticle);
        }
    };

    private Action rulerReticleAction = new AbstractAction(org.openpnp.Translations.getString("Local.b2eb0fa2b0167b6c")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            RulerReticle reticle = new RulerReticle();
            JMenu optionsMenu = createRulerReticleOptionsMenu(reticle);
            setReticleOptionsMenu(optionsMenu);
            cameraView.setDefaultReticle(reticle);
        }
    };

    private Action fiducialReticleAction = new AbstractAction(org.openpnp.Translations.getString("Local.8e8f55e644ab48c3")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            FiducialReticle reticle = new FiducialReticle();
            JMenu optionsMenu = createFiducialReticleOptionsMenu(reticle);
            setReticleOptionsMenu(optionsMenu);
            cameraView.setDefaultReticle(reticle);
        }
    };

    /**
     * Listen for menu selection to estimate an object's height
     */
    private ActionListener estimateZCoordinateAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                new EstimateObjectZCoordinateProcess(MainFrame.get(), cameraView);
            });
        }
    };

    /**
     * Listener for menu selection to move the selected nozzle to the camera (only works for
     * non-movable cameras)
     */
    private ActionListener moveSelectedNozzleToCameraAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                // Get the selected nozzle
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                // Add the offsets to the Camera's nozzle calibrated position.
                Location location = cameraView.getCamera().getLocation(nozzle);
                // Don't change rotation. 
                location = nozzle.getLocation().derive(location, true, true, true, false);
                // Move the nozzle to the camera
                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };

}
