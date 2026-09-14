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

package org.openpnp.gui.support;

import org.openpnp.spi.Actuator;

public class ActuatorItem extends HeadMountableItem {
    public ActuatorItem(Actuator actuator) {
        super(actuator);
    }

    public Actuator getActuator() {
        return (Actuator) hm;
    }

    @Override
    public String toString() {
        return String.format(org.openpnp.Translations.getString("Local.7626d88f21f17a80"), hm.getName(), hm.getHead() != null
                ? String.format(org.openpnp.Translations.getString("Local.fcad9d2874efd61d"), hm.getHead().getName()) : "");
    }
}
