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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.modbus.ModbusPollers;
import org.openhab.binding.mykitaheatpump.internal.modbus.ModbusWriter;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.core.io.transport.modbus.AsyncModbusWriteResult;
import org.openhab.core.io.transport.modbus.ModbusManager;
import org.openhab.core.io.transport.modbus.ModbusResponse;
import org.openhab.core.io.transport.modbus.ModbusWriteCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyKitaHeatPumpHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marco Tombesi - Initial contribution
 */
@NonNullByDefault
public class MyKitaHeatPumpHandler extends BaseThingHandler implements MyKitaHeatPumpThingHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Nullable MyKitaHeatPumpConfiguration config;
    private @Nullable ModbusTCPSlaveEndpoint endpoint;
    private @Nullable EndpointPoolConfiguration poolConfiguration;

    protected Supplier<ModbusManager> managerRef;

    // @Nullable
    // volatile DataValuePoller poller;

    final ModbusPollers modbusPollers;
    final ModbusWriter modbusWriter;
    final KitaHeatPump kita;
    final ChannelsHandler channelsHandler;

    public MyKitaHeatPumpHandler(Thing thing, Supplier<ModbusManager> managerRef) {
        super(thing);
        this.managerRef = managerRef;
        this.kita = new KitaHeatPump();
        this.modbusPollers = new ModbusPollers(kita, this);
        this.modbusWriter = new ModbusWriter(kita, this);
        this.channelsHandler = new ChannelsHandler(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        logger.debug("handleCommand {} {}", channelUID, command);
        /*
         * if (CHANNEL_1.equals(channelUID.getId())) {
         * if (command instanceof RefreshType) {
         * // TODO: handle data refresh
         * }
         *
         * // TODO: handle command
         *
         * // Note: if communication with thing fails for some reason,
         * // indicate that by setting the status with detail information:
         * // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
         * // "Could not control device at IP address x.x.x.x");
         * }
         */

        if (command != null) {

            if (command instanceof RefreshType) {
                // TODO: handle data refresh

                this.updateStatus(ThingStatus.ONLINE);

            } else {
                String kitaDataId = channelUID.getId();

                this.modbusWriter.writeData(kitaDataId, command, new ModbusWriteCallback() {

                    @Override
                    public void onError(ModbusWriteRequestBlueprint request, Exception error) {
                        logger.error("Write FAILED Command: {} \n\t Request: {} \n\t ERROR: {}", command, request,
                                error);

                        MyKitaHeatPumpHandler.this.updateThingStatus(ThingStatus.OFFLINE,
                                ThingStatusDetail.COMMUNICATION_ERROR,
                                String.format("Error (%s) with read. Request: %s. Description: %s. Message: %s",
                                        error.getClass().getSimpleName(), request, error.toString(),
                                        error.getMessage()));

                    }

                    @Override
                    public void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response) {
                        // TODO Auto-generated method stub
                        logger.debug("Write OK Command: {} \n\t Request: {} \n\t Response: {}", command, request,
                                response);

                        MyKitaHeatPumpHandler.this.updateThingStatus(ThingStatus.ONLINE, null, null);

                        if (command instanceof State) {
                            MyKitaHeatPumpHandler.this.tryUpdateChannelState(channelUID, ((State) command));
                        }

                    }

                    @Override
                    public void handle(AsyncModbusWriteResult result) {
                        // TODO Auto-generated method stub

                    }

                });

            }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////

    private void configure() throws ModbusConfigurationException {
        // logger.debug("Start initializing!");
        MyKitaHeatPumpConfiguration config = getConfigAs(MyKitaHeatPumpConfiguration.class);

        String host = config.host;
        if (host == null) {
            throw new ModbusConfigurationException("host must be non-null!");
        }

        this.config = config;
        this.endpoint = new ModbusTCPSlaveEndpoint(host, config.port);

        EndpointPoolConfiguration poolConfiguration = new EndpointPoolConfiguration();
        this.poolConfiguration = poolConfiguration;
        poolConfiguration.setConnectMaxTries(config.connectMaxTries);
        poolConfiguration.setConnectTimeoutMillis(config.connectTimeoutMillis);
        poolConfiguration.setInterConnectDelayMillis(config.timeBetweenReconnectMillis);
        poolConfiguration.setInterTransactionDelayMillis(config.timeBetweenTransactionsMillis);
        poolConfiguration.setReconnectAfterMillis(config.reconnectAfterMillis);

    }

    @Override
    synchronized public void initialize() {
        logger.trace("Initializing {} from status {}", this.getThing().getUID(), this.getThing().getStatus());
        if (this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            // If was online then first change it to offline.
            // this ensures that children will be notified about the change
            updateStatus(ThingStatus.OFFLINE);
        }
        try {

            this.configure();

            List<Channel> channels = ChannelsBuilder.of(this.kita, this.channelsHandler).build();
            Thing newThing = this.editThing().withChannels(channels).build();
            this.updateThing(newThing);

            if (this.config != null) {
                channelsHandler.configure(this.config);
            }

            @Nullable
            ModbusTCPSlaveEndpoint endpoint = this.endpoint;
            if (endpoint == null) {
                throw new IllegalArgumentException("endpoint null after configuration!");
            }
            managerRef.get().addListener(this);
            managerRef.get().setEndpointPoolConfiguration(endpoint, poolConfiguration);

            modbusPollers.initializePollers();

            updateStatus(ThingStatus.ONLINE);
        } catch (ModbusConfigurationException e) {
            logger.debug("Exception during initialization", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String
                    .format("Exception during initialization: %s (%s)", e.getMessage(), e.getClass().getSimpleName()));
        } finally {
            logger.trace("initialize() of thing {} '{}' finished", thing.getUID(), thing.getLabel());
        }

        /*
         * // The framework requires you to return from this method quickly. Also, before leaving this method a thing
         * // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
         * // case you can decide it directly.
         * // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
         * // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in
         * the
         * // background.
         *
         * // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
         * // the framework is then able to reuse the resources from the thing handler initialization.
         * // we set this upfront to reliably check status updates in unit tests.
         * updateStatus(ThingStatus.UNKNOWN);
         *
         * // Example for background initialization:
         * scheduler.execute(() -> {
         * boolean thingReachable = true; // <background task with long running initialization here>
         * // when done do:
         * if (thingReachable) {
         * updateStatus(ThingStatus.ONLINE);
         * } else {
         * updateStatus(ThingStatus.OFFLINE);
         * }
         * });
         *
         * // logger.debug("Finished initializing!");
         *
         * // Note: When initialization can NOT be done set the status with more details for further
         * // analysis. See also class ThingStatusDetail for all available status details.
         * // Add a description to give user information to understand why thing does not work as expected. E.g.
         * // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
         * // "Can not access device as username and/or password are invalid");
         */
    }

    @Override
    synchronized public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint otherEndpoint,
            @Nullable EndpointPoolConfiguration otherPoolConfiguration) {
        if (endpoint == null) {
            return;
        }
        EndpointPoolConfiguration poolConfiguration = this.poolConfiguration;
        if (poolConfiguration != null && otherEndpoint.equals(this.endpoint)
                && !poolConfiguration.equals(otherPoolConfiguration)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    this.formatConflictingParameterError(otherPoolConfiguration));
        }

    }

    private String formatConflictingParameterError(@Nullable EndpointPoolConfiguration otherPoolConfig) {
        return String.format(
                "Endpoint '%s' has conflicting parameters: parameters of this thing (%s '%s') %s are different from some other things parameter: %s. Ensure that all endpoints pointing to tcp slave '%s:%s' have same parameters.",
                endpoint, thing.getUID(), this.thing.getLabel(), this.poolConfiguration, otherPoolConfig,
                Optional.ofNullable(this.endpoint).map(e -> e.getAddress()).orElse("<null>"),
                Optional.ofNullable(this.endpoint).map(e -> String.valueOf(e.getPort())).orElse("<null>"));
    }

    @Override
    public int getSlaveId() {
        if (config != null) {
            return this.config.id;
        } else {
            throw new IllegalStateException("Not configured, but slave id is queried!");
        }
    }

    @Override
    public ThingUID getUID() {
        return getThing().getUID();
    }

    @Override
    public ModbusSlaveEndpoint asSlaveEndpoint() {
        if (this.endpoint != null) {
            return this.endpoint;
        }

        throw new RuntimeException("Modbus Slave Endpoint not configured");
    }

    @Override
    public Supplier<ModbusManager> getManagerRef() {
        return this.managerRef;
    }

    @Override
    public boolean isDiscoveryEnabled() {
        if (config != null) {
            return this.config.enableDiscovery;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void dispose() {
        logger.debug("dispose()");
        // if (this.poller != null) {
        // this.poller.unregisterPollTask();
        // }
        // this.callbackDelegator.resetCache();

        this.modbusPollers.dispose();
        this.channelsHandler.dispose();

        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public boolean hasConfigurationError() {
        ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR;
    }

    @Override
    public @Nullable MyKitaHeatPumpConfiguration getConfiguration() {
        return this.config;
    }

    @Override
    public ThingStatusInfo getStatusInfo() {

        return this.getThing().getStatusInfo();
    }

    @Override
    public void updateThingStatus(ThingStatus status, @Nullable ThingStatusDetail communicationError,
            @Nullable String format) {
        super.updateStatus(status, communicationError != null ? communicationError : ThingStatusDetail.NONE, format);

    }

    @Override
    public void tryUpdateChannelState(ChannelUID uid, State state) {
        try {
            this.updateState(uid, state);
        } catch (IllegalArgumentException e) {
            logger.warn("Error updating state '{}' (type {}) to channel {}: {} {}", state,
                    Optional.ofNullable(state).map(s -> s.getClass().getName()).orElse("null"), uid,
                    e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ChannelsHandler getChannelsHandler() {
        return this.channelsHandler;
    }

}
