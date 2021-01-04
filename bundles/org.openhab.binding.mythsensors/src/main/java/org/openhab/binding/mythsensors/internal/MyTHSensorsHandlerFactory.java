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
package org.openhab.binding.mythsensors.internal;

import static org.openhab.binding.mythsensors.internal.MyTHSensorsBindingConstants.THING_TYPE_THSENSOR;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mythsensors.internal.discovery.MyTHSensorsDiscoveryService;
import org.openhab.core.io.transport.modbus.ModbusManager;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyTHSensorsHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Marco Tombesi - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.mythsensors", service = ThingHandlerFactory.class)
public class MyTHSensorsHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_THSENSOR);

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Nullable MyTHSensorsDiscoveryService discovery;
    private @Nullable ModbusManager manager;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_THSENSOR.equals(thingTypeUID) && manager != null) {
            return new MyTHSensorsHandler(thing, manager);
        }

        return null;
    }

    @Override
    @Activate
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        Dictionary<String, Object> properties = componentContext.getProperties();
        logger.debug("MyTHSensorsHandlerFactory.activate componentContext \n\t {}", properties);

        if (properties != null) {
            String host = (String) properties.get("host");
            logger.debug("MyTHSensorsHandlerFactory.activate host: {}", host);
        }
    }

    @Reference
    protected void bindDiscovery(MyTHSensorsDiscoveryService discovery) {
        this.discovery = discovery;
    }

    protected void unbindDiscovery(MyTHSensorsDiscoveryService discovery) {
        this.discovery = null;
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
