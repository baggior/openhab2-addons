package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.RegisterTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaManager;
import org.openhab.core.io.transport.modbus.AsyncModbusFailure;
import org.openhab.core.io.transport.modbus.AsyncModbusWriteResult;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.ModbusWriteCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.io.transport.modbus.exception.ModbusConnectionException;
import org.openhab.core.io.transport.modbus.exception.ModbusTransportException;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusWriter extends ModbusManagerBase {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ModbusWriter(KitaManager kitaManager) {
        super(kitaManager);
    }

    public void writeData(String kitaDataId, Command originalCommand, ModbusWriteCallback modbusWriteCallback) {
        MyKitaHeatPumpConfiguration config = this.kitaManager.getKitaHeatPumpThingHandler().getConfiguration();

        if (config != null) {
            KitaHeatPumpDataItem dataType = this.kitaManager.getKita().getDataType(kitaDataId);
            if (dataType != null) {

                Command command = this.convertCommand(dataType, originalCommand);

                ModbusWriteRequestBlueprint request = this.requestFromCommand(dataType.register, dataType.address,
                        command, config);

                if (request != null) {
                    var comms = this.kitaManager.getCommunicationInterface();
                    logger.trace("Submitting write task {} to endpoint {}", request, comms.getEndpoint());
                    comms.submitOneTimeWrite(request, this::onWriteResponse, this::onWriteError);

                }
            }
        }

    }

    private @Nullable ModbusWriteRequestBlueprint requestFromCommand(RegisterTypeEnum kitaDataRegister,
            int kitaDataAddress, Command command, MyKitaHeatPumpConfiguration config) {

        ModbusWriteRequestBlueprint request = null;

        int slaveId = config.id;
        int maxTries = 1; // TODO config.getWriteMaxTries()

        // KitaHeatPumpDataType dataType = kita.getDataType(kitaDataId);

        Integer writeStart = kitaDataAddress;

        if (kitaDataRegister == RegisterTypeEnum.coil || kitaDataRegister == RegisterTypeEnum.discrete_input) {

            Optional<Boolean> commandAsBoolean = ModbusBitUtilities.translateCommand2Boolean(command);
            if (!commandAsBoolean.isPresent()) {
                logger.warn(
                        "Cannot process command {} with channel related to kita data address {} since command is not OnOffType, OpenClosedType or Decimal trying to write to coil. Do not know how to convert to 0/1.",
                        command, kitaDataAddress);

            } else {

                boolean data = commandAsBoolean.get();
                request = new ModbusWriteCoilRequestBlueprint(slaveId, writeStart, data, false, maxTries);
            }

        } else // if (dataType.register == RegisterTypeEnum.holding)
        {
            ModbusConstants.ValueType writeValueType = ModbusConstants.ValueType.INT16;
            ModbusRegisterArray data = ModbusBitUtilities.commandToRegisters(command, writeValueType);

            boolean writeMultiple = data.size() > 1;
            request = new ModbusWriteRegisterRequestBlueprint(slaveId, writeStart, data, writeMultiple, maxTries);
        }

        return request;
    }

    private Command convertCommand(KitaHeatPumpDataItem dataType, Command originalCommand) {

        double factor = this.writeFactor(dataType);

        if (originalCommand instanceof QuantityType) {
            DecimalType convertedCommand = new DecimalType(((Number) originalCommand).doubleValue() * factor);
            return convertedCommand;
        } else if (originalCommand instanceof DecimalType && factor != 1) {
            DecimalType convertedCommand = new DecimalType(((DecimalType) originalCommand).doubleValue() * factor);
            return convertedCommand;
        }
        return originalCommand;
    }

    private double writeFactor(KitaHeatPumpDataItem dataType) {
        if (dataType.register == RegisterTypeEnum.holding) {
            switch (dataType.type) {
                case _bool:
                case _switch:
                    return 1;

                case dateTime:
                    break;
                case number:
                    break;
                case string:
                    break;

                case cop:
                case flow_ro:
                case pct:
                case temperature_ro:
                case temperature_rw:
                case rps_ro:
                    return 10;
            }
        }
        return 1;
    }

    private synchronized void onWriteResponse(AsyncModbusWriteResult result) {
        if (!this.kitaManager.isActive()) {
            logger.warn("kitaManager not active");
            return;
        }
        logger.debug("Successful write, matching request {}", result.getRequest());

        ChannelUID lastWriteSuccessUID = this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler()
                .getChannelUID(MyKitaHeatPumpBindingConstants.CHANNEL_LAST_WRITE_SUCCESS);
        if (lastWriteSuccessUID != null) {
            this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler().updateChannelState(lastWriteSuccessUID,
                    new DateTimeType());
        }
    }

    private synchronized void onWriteError(AsyncModbusFailure<ModbusWriteRequestBlueprint> failure) {
        onError(failure.getRequest(), failure.getCause());
    }

    private synchronized void onError(ModbusWriteRequestBlueprint request, Exception error) {
        if (!this.kitaManager.isActive()) {
            logger.warn("kitaManager not active");
            return;
        }
        if (error instanceof ModbusConnectionException) {
            logger.debug("Thing {} '{}' had {} error on write: {}",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else if (error instanceof ModbusTransportException) {
            logger.debug("Thing {} '{}' had {} error on write: {}",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else {
            logger.error(
                    "Thing {} '{}' had {} error on write: {} (message: {}). Stack trace follows since this is unexpected error.",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(), error.getClass().getName(),
                    error.toString(), error.getMessage(), error);
        }

        ChannelUID lastWriteErrorUID = this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler()
                .getChannelUID(MyKitaHeatPumpBindingConstants.CHANNEL_LAST_WRITE_ERROR);
        if (lastWriteErrorUID != null) {
            this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler().updateChannelState(lastWriteErrorUID,
                    new DateTimeType());
        }

    }

}
