package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class ReadCallback implements ModbusReadCallback {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     *
     */
    // private final DataValuePoller dataValuePoller;
    // private final ChannelsHandler channelsHandler;
    private final ModbusPollers modbusPollers;

    /**
     * @param modbusPollers
     */
    ReadCallback(ModbusPollers modbusPollers) {

        this.modbusPollers = modbusPollers;
        // this.channelsHandler = modbusPollers.myThingHandler.getChannelsHandler();
    }

    private @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, ModbusRegisterArray> lastRegisters;
    private @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, BitArray> lastCoils;
    private @Nullable AtomicStampedKeyValue<ModbusReadRequestBlueprint, Exception> lastError;

    boolean disposed = false;

    @Override
    public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        // Ignore all incoming data and errors if configuration is not correct
        if (this.modbusPollers.myThingHandler.hasConfigurationError() || this.disposed) {
            return;
        }

        MyKitaHeatPumpConfiguration config = this.modbusPollers.myThingHandler.getConfiguration();
        if (config != null && config.cacheMillis >= 0) {
            AtomicStampedKeyValue<ModbusReadRequestBlueprint, ModbusRegisterArray> lastRegisters = this.lastRegisters;
            if (lastRegisters == null) {
                this.lastRegisters = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, registers);
            } else {
                lastRegisters.update(System.currentTimeMillis(), request, registers);
            }
        }
        this.logger.debug("Thing {} received registers {} for request {}", this.modbusPollers.myThingHandler.getUID(),
                registers, request);
        resetCommunicationError();
        // childCallbacks.forEach(handler -> handler.onRegisters(request, registers));

        this.modbusPollers.processUpdateStates(request, registers);

    }

    @Override
    public void onBits(ModbusReadRequestBlueprint request, BitArray coils) {
        // Ignore all incoming data and errors if configuration is not correct
        if (this.modbusPollers.myThingHandler.hasConfigurationError() || this.disposed) {
            return;
        }
        MyKitaHeatPumpConfiguration config = this.modbusPollers.myThingHandler.getConfiguration();
        if (config != null && config.cacheMillis >= 0) {
            AtomicStampedKeyValue<ModbusReadRequestBlueprint, BitArray> lastCoils = this.lastCoils;
            if (lastCoils == null) {
                this.lastCoils = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, coils);
            } else {
                lastCoils.update(System.currentTimeMillis(), request, coils);
            }
        }
        this.logger.debug("Thing {} received coils {} for request {}", this.modbusPollers.myThingHandler.getUID(),
                coils, request);
        resetCommunicationError();
        // childCallbacks.forEach(handler -> handler.onBits(request, coils));

        this.modbusPollers.processUpdateStates(request, coils);

    }

    @Override
    public void onError(ModbusReadRequestBlueprint request, Exception error) {

        // Ignore all incoming data and errors if configuration is not correct
        if (this.modbusPollers.myThingHandler.hasConfigurationError() || this.disposed) {
            return;
        }

        MyKitaHeatPumpConfiguration config = this.modbusPollers.myThingHandler.getConfiguration();
        if (config != null && config.cacheMillis >= 0) {
            AtomicStampedKeyValue<ModbusReadRequestBlueprint, Exception> lastError = this.lastError;
            if (lastError == null) {
                this.lastError = new AtomicStampedKeyValue<>(System.currentTimeMillis(), request, error);
            } else {
                lastError.update(System.currentTimeMillis(), request, error);
            }
        }

        this.logger.debug("Thing {} received error {} for request {}", this.modbusPollers.myThingHandler.getUID(),
                error, request);
        // childCallbacks.forEach(handler -> handler.onError(request, error));
        this.modbusPollers.myThingHandler.updateThingStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                String.format("Error with read: %s: %s", error.getClass().getName(), error.getMessage()));
    }

    private void resetCommunicationError() {
        ThingStatusInfo statusInfo = this.modbusPollers.myThingHandler.getStatusInfo();
        if (ThingStatus.OFFLINE.equals(statusInfo.getStatus())
                && ThingStatusDetail.COMMUNICATION_ERROR.equals(statusInfo.getStatusDetail())) {
            this.modbusPollers.myThingHandler.updateThingStatus(ThingStatus.ONLINE, null, null);
        }
    }

    private ThingUID getThingUID() {
        return this.modbusPollers.myThingHandler.getUID();
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
        ReadCallback rhs = (ReadCallback) obj;
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
    /*
     * public boolean updateChildrenWithOldData(long oldestStamp) {
     * AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object> lastData = getLastData();
     * if (lastData == null) {
     * return false;
     * }
     * AtomicStampedKeyValue<ModbusReadRequestBlueprint, Object> atomicData = lastData
     * .copyIfStampAfter(oldestStamp);
     * if (atomicData == null) {
     * return false;
     * }
     * ModbusReadRequestBlueprint request = atomicData.getKey();
     * logger.debug("Thing {} received data {} for request {}. Reusing cached data.",
     * DataValuePoller.this.myThingHandler.getUID(), atomicData.getValue(), request);
     * if (atomicData.getValue() instanceof ModbusRegisterArray) {
     * ModbusRegisterArray registers = (ModbusRegisterArray) atomicData.getValue();
     * childCallbacks.forEach(handler -> handler.onRegisters(atomicData.getKey(), registers));
     * } else if (atomicData.getValue() instanceof BitArray) {
     * BitArray coils = (BitArray) atomicData.getValue();
     * childCallbacks.forEach(handler -> handler.onBits(request, coils));
     * } else {
     * Exception error = (Exception) atomicData.getValue();
     * childCallbacks.forEach(handler -> handler.onError(request, error));
     * }
     * return true;
     * }
     */

    /**
     * Rest data caches
     */
    public void resetCache() {
        lastRegisters = null;
        lastCoils = null;
        lastError = null;
    }
}