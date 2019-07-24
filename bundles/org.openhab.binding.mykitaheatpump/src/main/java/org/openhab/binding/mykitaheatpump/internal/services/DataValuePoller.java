package org.openhab.binding.mykitaheatpump.internal.services;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.AtomicStampedKeyValue;
import org.openhab.io.transport.modbus.BasicModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.BasicPollTaskImpl;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataValuePoller {

    /**
     * {@link ModbusReadCallback} that delegates all tasks forward.
     *
     * All instances of {@linkplain ReadCallbackDelegator} are considered equal, if they are connected to the same
     * bridge. This makes sense, as the callback delegates
     * to all child things of this bridge.
     *
     * @author Sami Salonen
     *
     */
    private class ReadCallbackDelegator implements ModbusReadCallback {

        private volatile @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, ModbusRegisterArray> lastRegisters;
        private volatile @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, BitArray> lastCoils;
        private volatile @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, Exception> lastError;

        @Override
        public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
            // Ignore all incoming data and errors if configuration is not correct
            if (DataValuePoller.this.myThingHandler.hasConfigurationError() || disposed) {
                return;
            }

            MyKitaHeatPumpConfiguration config = DataValuePoller.this.myThingHandler.getConfiguration();
            if (config != null && config.cacheMillis >= 0) {
                AtomicStampedKeyValue<ModbusReadRequestBlueprint, ModbusRegisterArray> lastRegisters = this.lastRegisters;
                if (lastRegisters == null) {
                    this.lastRegisters = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, registers);
                } else {
                    lastRegisters.update(System.currentTimeMillis(), request, registers);
                }
            }
            logger.debug("Thing {} received registers {} for request {}", DataValuePoller.this.myThingHandler.getUID(),
                    registers, request);
            resetCommunicationError();
            childCallbacks.forEach(handler -> handler.onRegisters(request, registers));
        }

        @Override
        public void onBits(ModbusReadRequestBlueprint request, BitArray coils) {
            // Ignore all incoming data and errors if configuration is not correct
            if (DataValuePoller.this.myThingHandler.hasConfigurationError() || disposed) {
                return;
            }
            MyKitaHeatPumpConfiguration config = DataValuePoller.this.myThingHandler.getConfiguration();
            if (config != null && config.cacheMillis >= 0) {
                AtomicStampedKeyValue<ModbusReadRequestBlueprint, BitArray> lastCoils = this.lastCoils;
                if (lastCoils == null) {
                    this.lastCoils = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, coils);
                } else {
                    lastCoils.update(System.currentTimeMillis(), request, coils);
                }
            }
            logger.debug("Thing {} received coils {} for request {}", DataValuePoller.this.myThingHandler.getUID(),
                    coils, request);
            resetCommunicationError();
            childCallbacks.forEach(handler -> handler.onBits(request, coils));
        }

        @Override
        public void onError(ModbusReadRequestBlueprint request, Exception error) {

            // Ignore all incoming data and errors if configuration is not correct
            if (DataValuePoller.this.myThingHandler.hasConfigurationError() || disposed) {
                return;
            }

            MyKitaHeatPumpConfiguration config = DataValuePoller.this.myThingHandler.getConfiguration();
            if (config != null && config.cacheMillis >= 0) {
                AtomicStampedKeyValue<ModbusReadRequestBlueprint, Exception> lastError = this.lastError;
                if (lastError == null) {
                    this.lastError = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, error);
                } else {
                    lastError.update(System.currentTimeMillis(), request, error);
                }
            }

            logger.debug("Thing {} received error {} for request {}", DataValuePoller.this.myThingHandler.getUID(),
                    error, request);
            childCallbacks.forEach(handler -> handler.onError(request, error));
            DataValuePoller.this.myThingHandler.updateThingStatus(ThingStatus.OFFLINE,
                    ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error with read: %s: %s", error.getClass().getName(), error.getMessage()));
        }

        private void resetCommunicationError() {
            ThingStatusInfo statusInfo = DataValuePoller.this.myThingHandler.getStatusInfo();
            if (ThingStatus.OFFLINE.equals(statusInfo.getStatus())
                    && ThingStatusDetail.COMMUNICATION_ERROR.equals(statusInfo.getStatusDetail())) {
                DataValuePoller.this.myThingHandler.updateThingStatus(ThingStatus.ONLINE, null, null);
            }
        }

        private ThingUID getThingUID() {
            return DataValuePoller.this.myThingHandler.getUID();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            ReadCallbackDelegator rhs = (ReadCallbackDelegator) obj;
            return getThingUID().equals(rhs.getThingUID());
        }

        @Override
        public int hashCode() {
            return getThingUID().hashCode();
        }

        @SuppressWarnings("unchecked")
        private @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object> getLastData() {
            try {
                return (AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object>) Stream
                        .of(lastRegisters, lastCoils, lastError).max(AtomicStampedKeyValue::compare).get();
            } catch (NullPointerException e) {
                // max (latest) element is null -> all data are null
                return null;
            }
        }

        /**
         * Update children data if data is fresh enough
         *
         * @param oldestStamp oldest data that is still passed to children
         * @return whether data was updated. Data is not updated when it's too old or there's no data at all.
         */
        public boolean updateChildrenWithOldData(long oldestStamp) {
            AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object> lastData = getLastData();
            if (lastData == null) {
                return false;
            }
            AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object> atomicData = lastData
                    .copyIfStampAfter(oldestStamp);
            if (atomicData == null) {
                return false;
            }
            ModbusReadRequestBlueprint request = atomicData.getKey();
            logger.debug("Thing {} received data {} for request {}. Reusing cached data.",
                    DataValuePoller.this.myThingHandler.getUID(), atomicData.getValue(), request);
            if (atomicData.getValue() instanceof ModbusRegisterArray) {
                ModbusRegisterArray registers = (ModbusRegisterArray) atomicData.getValue();
                childCallbacks.forEach(handler -> handler.onRegisters(atomicData.getKey(), registers));
            } else if (atomicData.getValue() instanceof BitArray) {
                BitArray coils = (BitArray) atomicData.getValue();
                childCallbacks.forEach(handler -> handler.onBits(request, coils));
            } else {
                Exception error = (Exception) atomicData.getValue();
                childCallbacks.forEach(handler -> handler.onError(request, error));
            }
            return true;
        }

        /**
         * Rest data caches
         */
        public void resetCache() {
            lastRegisters = null;
            lastCoils = null;
            lastError = null;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final @NonNull MyKitaHeatPumpThingHandler myThingHandler;

    private final @NonNull ModbusReadFunctionCode functionCode;

    int start;
    int length;

    // ModbusMasterService modbusService;

    private volatile @Nullable PollTask pollTask;

    @NonNull
    Supplier<@NonNull ModbusManager> managerRef;

    private volatile boolean disposed;

    private volatile List<ModbusReadCallback> childCallbacks = new CopyOnWriteArrayList<>();

    private final ReadCallbackDelegator callbackDelegator = new ReadCallbackDelegator();

    DataValuePoller(@NonNull MyKitaHeatPumpThingHandler myThingHandler, @NonNull ModbusReadFunctionCode functionCode,
            int start, int length) {

        this.myThingHandler = myThingHandler;
        this.managerRef = myThingHandler.getManagerRef();

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
            logger.info("Unregistering polling from ModbusManager");
            @NonNull
            PollTask task = (@NonNull PollTask) pollTask;
            managerRef.get().unregisterRegularPoll(task);
            pollTask = null;
            this.callbackDelegator.resetCache();
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
        this.callbackDelegator.resetCache();

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
        PollTask task = new BasicPollTaskImpl(myThingHandler.asSlaveEndpoint(), request, this.callbackDelegator);
        this.pollTask = task;

        if (config.refresh <= 0L) {
            logger.debug("Not registering polling with ModbusManager since refresh disabled");

        } else {
            logger.info("Registering polling with ModbusManager:\n function {}, start {}, length {}, refresh {}",
                    functionCode, start, length, config.refresh);
            managerRef.get().registerRegularPoll(task, config.refresh, 0);

        }
    }

}
