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

import static org.openhab.binding.mythsensors.internal.MyTHSensorsBindingConstants.CHANNEL_1;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.mythsensors.internal.modbus.ModbusMasterService;
import org.openhab.binding.mythsensors.internal.modbus.ModbusPollers;
import org.openhab.io.transport.modbus.ModbusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyTHSensorsHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marco Tombesi - Initial contribution
 */
@NonNullByDefault
public class MyTHSensorsHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MyTHSensorsHandler.class);

    private @Nullable MyTHSensorsConfiguration config;
    private @Nullable ModbusMasterService modbusMasterService;
    private @Nullable ModbusPollers modbusPollers;

    private final ModbusManager manager;

    public MyTHSensorsHandler(Thing thing, ModbusManager manager) {
        super(thing);
        this.manager = manager;

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {

        logger.trace("Initializing {} from status {}", this.getThing().getUID(), this.getThing().getStatus());
        if (this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            // If was online then first change it to offline.
            // this ensures that children will be notified about the change
            updateStatus(ThingStatus.OFFLINE);
        }
        try {
            config = getConfigAs(MyTHSensorsConfiguration.class);
            modbusMasterService = new ModbusMasterService(this.manager, this.getConfig().getProperties());
            modbusPollers = new ModbusPollers(this, modbusMasterService);

            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.debug("Exception during initialization", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String
                    .format("Exception during initialization: %s (%s)", e.getMessage(), e.getClass().getSimpleName()));
        } finally {
            logger.trace("initialize() of thing {} '{}' finished", thing.getUID(), thing.getLabel());
        }

    }

    public boolean hasConfigurationError() {
        ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR;
    }

    @Override
    public synchronized void dispose() {
        logger.debug("dispose()");
        // if (this.poller != null) {
        // this.poller.unregisterPollTask();
        // }
        // this.callbackDelegator.resetCache();

        // this.modbusPollers.dispose();
        // this.channelsHandler.dispose();

        updateStatus(ThingStatus.OFFLINE);
    }
}
