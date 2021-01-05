package org.openhab.binding.mykitaheatpump.internal.modbus;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.handler.MyKitaHeatPumpThingHandler;
import org.openhab.core.io.transport.modbus.AsyncModbusFailure;
import org.openhab.core.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.core.io.transport.modbus.ModbusFailureCallback;
import org.openhab.core.io.transport.modbus.ModbusReadCallback;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
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
@NonNullByDefault
class PollerModbusReadCallback implements ModbusReadCallback, ModbusFailureCallback<ModbusReadRequestBlueprint> {

    /**
     * Immutable data object to cache the results of a poll request
     */
    private class PollResult {

        public final @Nullable AsyncModbusReadResult result;
        public final @Nullable AsyncModbusFailure<ModbusReadRequestBlueprint> failure;

        PollResult(AsyncModbusReadResult result) {
            this.result = result;
            this.failure = null;
        }

        PollResult(AsyncModbusFailure<ModbusReadRequestBlueprint> failure) {
            this.result = null;
            this.failure = failure;
        }

        @Override
        public String toString() {
            return failure == null ? String.format("PollResult(result=%s)", result)
                    : String.format("PollResult(failure=%s)", failure);
        }
    }

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile @Nullable AtomicStampedValue<PollResult> lastResult;

    private final MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler;
    private final ModbusResponseHandler modbusResponseHandler;

    PollerModbusReadCallback(ModbusResponseHandler modbusResponseHandler,
            MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler) {
        this.modbusResponseHandler = modbusResponseHandler;
        this.kitaHeatPumpThingHandler = kitaHeatPumpThingHandler;
    }

    private void handleResult(PollResult result) {

        MyKitaHeatPumpConfiguration config = kitaHeatPumpThingHandler.getConfiguration();
        if (config != null && config.cacheMillis >= 0) {
            AtomicStampedValue<PollResult> localLastResult = this.lastResult;
            if (localLastResult == null) {
                this.lastResult = new AtomicStampedValue<>(System.currentTimeMillis(), result);
            } else {
                localLastResult.update(System.currentTimeMillis(), result);
                this.lastResult = localLastResult;
            }
        }
        logger.debug("Thing {} received response {}", kitaHeatPumpThingHandler.getThing().getUID(), result);

        if (result.failure != null) {
            Exception error = result.failure.getCause();
            assert error != null;
            // TODO comunicationError

        } else {
            resetCommunicationError();
        }
    }

    private void resetCommunicationError() {
        // TODO Auto-generated method stub

    }

    @Override
    public synchronized void handle(AsyncModbusReadResult result) {
        logger.debug("handle result: {}", result);
        // Ignore all incoming data and errors if configuration is not correct
        if (this.kitaHeatPumpThingHandler.hasConfigurationError() || this.kitaHeatPumpThingHandler.isDisposed()) {
            return;
        }
        this.handleResult(new PollResult(result));

        if (result.getRegisters().isPresent()) {
            this.modbusResponseHandler.processUpdateStates(result.getRequest(), result.getRegisters().get());
        } else if (result.getBits().isPresent()) {
            this.modbusResponseHandler.processUpdateStates(result.getRequest(), result.getBits().get());
        }
    }

    @Override
    public synchronized void handle(AsyncModbusFailure<ModbusReadRequestBlueprint> failure) {
        logger.debug("handle failure: {}", failure);
        // Ignore all incoming data and errors if configuration is not correct
        if (this.kitaHeatPumpThingHandler.hasConfigurationError() || this.kitaHeatPumpThingHandler.isDisposed()) {
            return;
        }

        this.handleResult(new PollResult(failure));

        this.modbusResponseHandler.processOnReadError(failure.getRequest(), failure.getCause());
    }

    /**
     * Reset data caches
     */
    public void resetCache() {
        lastResult = null;
    }
}
