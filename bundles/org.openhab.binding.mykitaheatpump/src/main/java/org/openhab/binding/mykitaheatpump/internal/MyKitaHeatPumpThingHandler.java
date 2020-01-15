/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.Identifiable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.mykitaheatpump.internal.channels.ChannelsCacheHandler;
import org.openhab.binding.mykitaheatpump.internal.modbus.ModbusHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;

/**
 * Base interface for thing handlers of endpoint things
 *
 *
 */
@NonNullByDefault
public interface MyKitaHeatPumpThingHandler extends Identifiable<ThingUID> {

    /**
     * Gets the {@link ModbusSlaveEndpoint} represented by the thing
     *
     * Note that the endpoint can be <code>null</code> in case of incomplete initialization
     *
     * @return endpoint represented by this thing handler
     */
    // public ModbusSlaveEndpoint asSlaveEndpoint();

    // /**
    // * Get Slave ID, also called as unit id, represented by the thing
    // *
    // * @return slave id represented by this thing handler
    // * @throws EndpointNotInitializedException in case the initialization is not complete
    // */
    // public int getSlaveId();

    /**
     * Get {@link ModbusManager} supplier
     *
     * @return reference to ModbusManager
     */
    // public Supplier<ModbusManager> getManagerRef();

    /**
     * Return true if auto discovery is enabled for this endpoint
     *
     * @return boolean true if the discovery is enabled
     */
    public boolean isDiscoveryEnabled();

    public @Nullable MyKitaHeatPumpConfiguration getConfiguration();

    public void updateThingStatus(ThingStatus offline, @Nullable ThingStatusDetail communicationError,
            @Nullable String format);

    public boolean hasConfigurationError();

    public ThingStatusInfo getStatusInfo();

    public ChannelsCacheHandler getChannelsHandler();

    public void tryUpdateChannelState(ChannelUID uid, State state);

    public Thing getThing();

    public ModbusHandler getModbusHandler();

    public KitaHeatPump getKita();

    public void updateThingChannels(List<Channel> channels);
}
