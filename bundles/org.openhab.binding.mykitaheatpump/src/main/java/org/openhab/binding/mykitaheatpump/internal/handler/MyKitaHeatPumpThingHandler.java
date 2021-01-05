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
package org.openhab.binding.mykitaheatpump.internal.handler;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.State;

/**
 * Base interface for thing handlers of endpoint things
 *
 *
 */
@NonNullByDefault
public interface MyKitaHeatPumpThingHandler extends ThingHandler {

    /**
     * Gets the {@link ModbusSlaveEndpoint} represented by the thing
     *
     * Note that the endpoint can be <code>null</code> in case of incomplete initialization
     *
     * @return endpoint represented by this thing handler
     */
    public ModbusSlaveEndpoint asSlaveEndpoint();

    /**
     * Get Slave ID, also called as unit id, represented by the thing
     *
     * @return slave id represented by this thing handler
     * @throws EndpointNotInitializedException in case the initialization is not complete
     */
    public int getSlaveId();

    /**
     * Return true if auto discovery is enabled for this endpoint
     *
     * @return boolean true if the discovery is enabled
     */
    public boolean isDiscoveryEnabled();

    public @Nullable MyKitaHeatPumpConfiguration getConfiguration();

    public void updateThingStatus(ThingStatus offline, @Nullable ThingStatusDetail detail,
            @Nullable String description);

    public boolean hasConfigurationError();

    public ThingStatusInfo getStatusInfo();

    public void updateChannelState(ChannelUID uid, State state);

    public boolean isDisposed();

    public ChannelsHandler getChannelsHandler();

    public void editThingChannels(ArrayList<Channel> chennels);
}
