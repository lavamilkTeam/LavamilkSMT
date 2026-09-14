package org.openpnp.machine.photon.sheets;

import org.openpnp.machine.photon.sheets.gui.GlobalConfigConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;

import javax.swing.*;

public class GlobalConfigPropertySheet implements PropertySheetHolder.PropertySheet {
    @Override
    public String getPropertySheetTitle() {
        return org.openpnp.Translations.format("Local.6a676ba8822adc5d");
    }

    @Override
    public JPanel getPropertySheetPanel() {
    	return new GlobalConfigConfigurationWizard();
    }
}
