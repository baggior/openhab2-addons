/**
 * Copyright (c) 2019-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mykitaheatpump.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MyKitaHeatPumpBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marco Tombesi - Initial contribution
 */
@NonNullByDefault
public class MyKitaHeatPumpBindingConstants {

    public static final String BINDING_ID = "mykitaheatpump";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_KITA_REGISTERS = new ThingTypeUID(BINDING_ID, "kita");

    public static final ThingTypeUID THING_TYPE_TEST = new ThingTypeUID(BINDING_ID, "test");

}
