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

import static org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.mykitaheatpump.internal.test.handler.TestHandler;
import org.openhab.io.transport.modbus.ModbusManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyKitaHeatPumpHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Marco Tombesi - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.mykitaheatpump", service = ThingHandlerFactory.class)
public class MyKitaHeatPumpHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(MyKitaHeatPumpHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_KITA_REGISTERS);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_TEST);
    }

    @NonNullByDefault({})
    private ModbusManager manager;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_KITA_REGISTERS.equals(thingTypeUID)) {
            logger.info("Thing created label: '{}', uuid: {}", thing.getLabel(), thingTypeUID);
            return new MyKitaHeatPumpHandler(thing, () -> manager);
        } else if (THING_TYPE_TEST.equals(thingTypeUID)) {
            logger.info("Thing TEST created label: '{}', uuid: {}", thing.getLabel(), thingTypeUID);
            return new TestHandler(thing, () -> manager);
        }

        return null;
    }

    @Reference
    public void setModbusManager(ModbusManager manager) {
        logger.debug("Setting ModbusManager: {}", manager);
        this.manager = manager;
    }

    public void unsetModbusManager(ModbusManager manager) {
        this.manager = null;
    }
}
