package org.openhab.binding.mykitaheatpump.internal.services;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.io.transport.modbus.BasicModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.BasicPollTaskImpl;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.PollTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataValuePoller {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    final @NonNull MyKitaHeatPumpThingHandler myThingHandler;
    final @NonNull ModbusMasterService modbusMasterService;

    private final @NonNull ModbusReadFunctionCode functionCode;

    int start;
    int length;

    // ModbusMasterService modbusService;

    private @Nullable PollTask pollTask;

    volatile boolean disposed;

    // private volatile List<ModbusReadCallback> childCallbacks = new CopyOnWriteArrayList<>();

    private final ReadCallback callback;

    DataValuePoller(ModbusMasterService modbusMasterService, @NonNull ModbusReadFunctionCode functionCode, int start,
            int length) {

        this.modbusMasterService = modbusMasterService;
        this.myThingHandler = modbusMasterService.myThingHandler;

        this.callback = new ReadCallback(this);

        this.functionCode = functionCode;

        this.start = start;
        this.length = length;

        this.disposed = false;
    }

    synchronized void unregisterPollTask() {
        logger.trace("unregisterPollTask()");
        // Mark handler as disposed as soon as possible to halt processing of callbacks
        disposed = true;
        if (pollTask == null) {
            return;
        } else {
            logger.info("Unregistering polling from ModbusManager, function {}, start {}, length {}", this.functionCode,
                    this.start, this.length);
            @NonNull
            PollTask task = (@NonNull PollTask) pollTask;
            myThingHandler.getManagerRef().get().unregisterRegularPoll(task);
            pollTask = null;
            this.callback.resetCache();
        }

    }

    /**
     * Register poll task
     *
     * @throws EndpointNotInitializedException in case the bridge initialization is not complete. This should only
     *             happen in transient conditions, for example, when bridge is initializing.
     */
    synchronized void registerPollTask() {
        logger.trace("registerPollTask()");
        if (pollTask != null) {
            throw new RuntimeException("pollTask should be unregistered before registering a new one!");
        }
        this.callback.resetCache();

        // ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        // if (slaveEndpointThingHandler == null) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format("Bridge '%s' is offline",
        // Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>")));
        // logger.debug("No bridge handler available -- aborting init for {}", this);
        // return;
        // }
        // ModbusSlaveEndpoint endpoint = slaveEndpointThingHandler.asSlaveEndpoint();
        // if (endpoint == null) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format(
        // "Bridge '%s' not completely initialized", Optional.ofNullable(getBridge()).map(b -> b.getLabel())));
        // logger.debug("Bridge not initialized fully (no endpoint) -- aborting init for {}", this);
        // return;
        // }

        MyKitaHeatPumpConfiguration config = myThingHandler.getConfiguration();
        if (config == null) {
            throw new RuntimeException("MyKitaHeatPumpConfiguration cannot be null!");
        }

        BasicModbusReadRequestBlueprint request = new BasicModbusReadRequestBlueprint(config.id, this.functionCode,
                this.start, this.length, config.maxTries);

        @NonNull
        PollTask task = new BasicPollTaskImpl(myThingHandler.asSlaveEndpoint(), request, this.callback);
        this.pollTask = task;

        if (config.refresh <= 0L) {
            logger.debug("Not registering polling with ModbusManager since refresh disabled");

        } else {
            logger.info("Registering polling with ModbusManager:\n function {}, start {}, length {}, refresh {}",
                    functionCode, start, length, config.refresh);
            myThingHandler.getManagerRef().get().registerRegularPoll(task, config.refresh, 0);

        }
    }

}
