package org.openhab.binding.mykitaheatpump.internal.test.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.mykitaheatpump.internal.ModbusConfigurationException;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.io.transport.modbus.BasicModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.BasicPollTaskImpl;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTask;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*

*/
public class TestHandler extends BaseThingHandler implements ModbusManagerListener, ModbusReadCallback { // ,
                                                                                                         // MyKitaHeatPumpThingHandler
                                                                                                         // {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Nullable MyKitaHeatPumpConfiguration config;
    private @Nullable ModbusTCPSlaveEndpoint endpoint;
    private @Nullable EndpointPoolConfiguration poolConfiguration;

    protected Supplier<ModbusManager> managerRef;

    public TestHandler(Thing thing, Supplier<ModbusManager> managerRef) {
        super(thing);
        this.managerRef = managerRef;
        // this.kita = new KitaHeatPump();
        // this.modbusPollers = new ModbusPollers(kita, this);
        // this.modbusWriter = new ModbusWriter(kita, this);

    }

    @Override
    public synchronized void dispose() {
        logger.debug("dispose()");

        // this.modbusPollers.dispose();
        // this.channelsHandler.dispose();

        this.disposePollers();

        updateStatus(ThingStatus.OFFLINE);
    }

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

            List<Channel> channels = this.buildChannels();
            Thing newThing = this.editThing().withChannels(channels).build();
            this.updateThing(newThing);

            @Nullable
            ModbusTCPSlaveEndpoint endpoint = this.endpoint;
            if (endpoint == null) {
                throw new IllegalArgumentException("endpoint null after configuration!");
            }
            managerRef.get().addListener(this);
            managerRef.get().setEndpointPoolConfiguration(endpoint, poolConfiguration);

            this.initializePoolers();

            updateStatus(ThingStatus.ONLINE);
        } catch (ModbusConfigurationException e) {
            logger.debug("Exception during initialization", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String
                    .format("Exception during initialization: %s (%s)", e.getMessage(), e.getClass().getSimpleName()));
        } finally {
            logger.trace("initialize() of thing {} '{}' finished", thing.getUID(), thing.getLabel());
        }
    }

    private void initializePoolers() {

        final ModbusReadFunctionCode functionCode = ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS;
        final int start = 1;
        final int length = 123; // max 123 uint Modbus.MAX_MESSAGE_LENGTH =256 bytes

        if (this.config != null) {
            BasicModbusReadRequestBlueprint request = new BasicModbusReadRequestBlueprint(this.config.id, functionCode,
                    start, length, this.config.maxTries);
            if (this.endpoint != null) {
                PollTask task = new BasicPollTaskImpl(this.endpoint, request, this);

                if (this.config.refresh <= 0L) {
                    logger.debug("Not registering polling with ModbusManager since refresh disabled");

                } else {
                    logger.info(
                            "Registering polling with ModbusManager:\n function {}, start {}, length {}, refresh {}",
                            functionCode, start, length, this.config.refresh);

                    this.managerRef.get().registerRegularPoll(task, this.config.refresh, 0);

                }
            }
        }
    }

    private void disposePollers() {
        this.managerRef.get().getRegisteredRegularPolls().forEach(pollTask -> {
            ModbusReadRequestBlueprint requestBlueprint = pollTask.getRequest();

            logger.info("Unregistering polling from ModbusManager:\n unitId {}, function {}, start {}, length {}",
                    requestBlueprint.getUnitID(), requestBlueprint.getFunctionCode(), requestBlueprint.getReference(),
                    requestBlueprint.getDataLength());
            this.managerRef.get().unregisterRegularPoll(pollTask);
        });

    }

    @NonNull
    private List<Channel> buildChannels() {

        List<Channel> ret = new ArrayList<Channel>();

        final String channelId = "test-ch-id";
        final String label = "test channel";

        ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), channelId);
        ChannelTypeUID ctypeUUID = new ChannelTypeUID(MyKitaHeatPumpBindingConstants.BINDING_ID, "sample-channel");
        String acceptedItemType = "Number";

        ChannelBuilder builder = ChannelBuilder.create(channelUID, acceptedItemType).withLabel(label);
        builder.withType(ctypeUUID);
        Channel ch = builder.build();
        ret.add(ch);

        return ret;
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

                // this.modbusWriter.writeData(kitaDataId, command, new ModbusWriteCallback() {
                //
                // @Override
                // public void onError(ModbusWriteRequestBlueprint request, Exception error) {
                // logger.error("Write FAILED Command: {} \n\t Request: {} \n\t ERROR: {}", command, request,
                // error);
                //
                // MyKitaHeatPumpHandler.this.updateThingStatus(ThingStatus.OFFLINE,
                // ThingStatusDetail.COMMUNICATION_ERROR,
                // String.format("Error (%s) with read. Request: %s. Description: %s. Message: %s",
                // error.getClass().getSimpleName(), request, error.toString(),
                // error.getMessage()));
                //
                // }
                //
                // @Override
                // public void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response) {
                // // TODO Auto-generated method stub
                // logger.debug("Write OK Command: {} \n\t Request: {} \n\t Response: {}", command, request,
                // response);
                //
                // MyKitaHeatPumpHandler.this.updateThingStatus(ThingStatus.ONLINE, null, null);
                //
                // if (command instanceof State) {
                // MyKitaHeatPumpHandler.this.tryUpdateChannelState(channelUID, ((State) command));
                // }
                //
                // }
                //
                // });

            }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////

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

    // ModbusReadCallback implementations

    @Override
    public void onRegisters(@NonNull ModbusReadRequestBlueprint request, @NonNull ModbusRegisterArray registers) {
        // TODO Auto-generated method stub
        this.logger.debug("request <- {}\n response -> {}", request, registers);
        resetCommunicationError();
    }

    @Override
    public void onBits(@NonNull ModbusReadRequestBlueprint request, @NonNull BitArray bits) {
        // TODO Auto-generated method stub
        this.logger.debug("request <- {}\n response -> {}", request, bits);
        resetCommunicationError();

    }

    @Override
    public void onError(@NonNull ModbusReadRequestBlueprint request, @NonNull Exception error) {
        // TODO Auto-generated method stub
        this.logger.error("Thing {} received error {} for request {}", this.getThing().getUID(), error, request);

        this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                String.format("Error with read: %s: %s", error.getClass().getName(), error.getMessage()));

    }

    private void resetCommunicationError() {
        ThingStatusInfo statusInfo = this.getThing().getStatusInfo();
        if (ThingStatus.OFFLINE.equals(statusInfo.getStatus())
                && ThingStatusDetail.COMMUNICATION_ERROR.equals(statusInfo.getStatusDetail())) {
            this.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, null);
        }
    }
}
