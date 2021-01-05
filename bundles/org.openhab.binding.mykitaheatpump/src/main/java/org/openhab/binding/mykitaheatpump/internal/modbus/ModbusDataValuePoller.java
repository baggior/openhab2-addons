package org.openhab.binding.mykitaheatpump.internal.modbus;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.handler.MyKitaHeatPumpThingHandler;
import org.openhab.core.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.core.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.PollTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
class ModbusDataValuePoller {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // final @NonNull MyKitaHeatPumpThingHandler myThingHandler;
    // final @NonNull ModbusPollers modbusMasterService;

    int start;
    int length;
    final ModbusReadFunctionCode functionCode;
    private final int maxTries;
    private final long refresh;
    // ModbusMasterService modbusService;

    private @Nullable PollTask pollTask;

    private @Nullable ModbusReadRequestBlueprint request;

    // private volatile List<ModbusReadCallback> childCallbacks = new CopyOnWriteArrayList<>();

    ModbusDataValuePoller(ModbusReadFunctionCode functionCode, int start, int length, int maxtries, long refresh) {

        // this.modbusMasterService = modbusMasterService;
        // this.myThingHandler = modbusPollers.myThingHandler;

        this.functionCode = functionCode;

        this.start = start;
        this.length = length;
        this.maxTries = maxtries;
        this.refresh = refresh;

    }

    /**
     * Unregister poll task.
     *
     * No-op in case no poll task is registered, or if the initialization is incomplete.
     */
    public synchronized void unregisterPollTask(MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler,
            ModbusCommunicationInterface modbusInterface) {
        logger.trace("unregisterPollTask()");

        PollTask localPollTask = this.pollTask;
        if (localPollTask != null) {
            logger.debug("Unregistering polling from ModbusManager");
            modbusInterface.unregisterRegularPoll(localPollTask);
        }
        this.pollTask = null;
        this.request = null;
    }
    //
    // synchronized void unregisterPollTask() {
    // logger.trace("unregisterPollTask()");
    // // Mark handler as disposed as soon as possible to halt processing of callbacks
    // this.callback.disposed = true;
    // if (pollTask == null) {
    // return;
    // } else {
    // logger.info("Unregistering polling from ModbusManager, function {}, start {}, length {}", this.functionCode,
    // this.start, this.length);
    // @NonNull
    // PollTask task = (@NonNull PollTask) pollTask;
    // myThingHandler.getManagerRef().get().unregisterRegularPoll(task);
    // pollTask = null;
    // this.callback.resetCache();
    // }
    //
    // }

    /**
     * Register poll task
     *
     * @throws EndpointNotInitializedException in case the bridge initialization is not complete. This should only
     *             happen in transient conditions, for example, when bridge is initializing.
     */

    public synchronized void registerPollTask(ModbusResponseHandler modbusResponseHandler,
            MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler, ModbusCommunicationInterface modbusInterface) {

        if (kitaHeatPumpThingHandler.isDisposed()) {
            return;
        }

        logger.trace("registerPollTask()");
        if (pollTask != null) {
            String detail = "pollTask should be unregistered before registering a new one!";
            logger.debug(detail);
            return;
        }

        ModbusReadRequestBlueprint localRequest = new ModbusReadRequestBlueprint(kitaHeatPumpThingHandler.getSlaveId(),
                this.functionCode, this.start, this.length, this.maxTries);
        this.request = localRequest;

        if (this.refresh <= 0L) {
            logger.debug("Not registering polling with ModbusManager since refresh disabled");
        } else {
            logger.debug("Registering polling with ModbusManager");

            PollerModbusReadCallback callback = new PollerModbusReadCallback(modbusResponseHandler,
                    kitaHeatPumpThingHandler);
            pollTask = modbusInterface.registerRegularPoll(localRequest, this.refresh, 0, callback, callback);

            assert pollTask != null;
        }
    }

    /**
     * Register poll task
     *
     * @throws EndpointNotInitializedException in case the bridge initialization is not complete. This should only
     *             happen in transient conditions, for example, when bridge is initializing.
     */
    // synchronized void registerPollTask() {
    // logger.trace("registerPollTask()");
    // if (pollTask != null) {
    // throw new RuntimeException("pollTask should be unregistered before registering a new one!");
    // }
    // this.callback.resetCache();
    //
    // // ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
    // // if (slaveEndpointThingHandler == null) {
    // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format("Bridge '%s' is offline",
    // // Optional.ofNullable(getBridge()).map(b -> b.getLabel()).orElse("<null>")));
    // // logger.debug("No bridge handler available -- aborting init for {}", this);
    // // return;
    // // }
    // // ModbusSlaveEndpoint endpoint = slaveEndpointThingHandler.asSlaveEndpoint();
    // // if (endpoint == null) {
    // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format(
    // // "Bridge '%s' not completely initialized", Optional.ofNullable(getBridge()).map(b -> b.getLabel())));
    // // logger.debug("Bridge not initialized fully (no endpoint) -- aborting init for {}", this);
    // // return;
    // // }
    //
    // MyKitaHeatPumpConfiguration config = myThingHandler.getConfiguration();
    // if (config == null) {
    // throw new RuntimeException("MyKitaHeatPumpConfiguration cannot be null!");
    // }
    //
    // ModbusReadRequestBlueprint request = new ModbusReadRequestBlueprint(config.id, this.functionCode, this.start,
    // this.length, config.maxTries);
    //
    // @NonNull
    // PollTask task = new BasicPollTask(myThingHandler.asSlaveEndpoint(), request, this.callback);
    // this.pollTask = task;
    //
    // if (config.refresh <= 0L) {
    // logger.debug("Not registering polling with ModbusManager since refresh disabled");
    //
    // } else {
    // logger.info("Registering polling with ModbusManager:\n function {}, start {}, length {}, refresh {}",
    // functionCode, start, length, config.refresh);
    //
    // myThingHandler.getManagerRef().get().registerRegularPoll(task, config.refresh, 0);
    //
    // }
    // }

    @Override
    public String toString() {
        return "ModbusDataValuePoller [functionCode=" + functionCode + ", start=" + start + ", length=" + length + "]";
    }

}
