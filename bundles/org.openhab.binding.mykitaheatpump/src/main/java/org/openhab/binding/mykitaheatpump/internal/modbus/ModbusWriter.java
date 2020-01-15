package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;
import org.openhab.io.transport.modbus.BasicModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.BasicModbusWriteRegisterRequestBlueprint;
import org.openhab.io.transport.modbus.BasicWriteTask;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusResponse;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
class ModbusWriter {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    final MyKitaHeatPumpThingHandler myThingHandler;

    final KitaHeatPump kita;

    public ModbusWriter(MyKitaHeatPumpThingHandler myThingHandler) {
        this.myThingHandler = myThingHandler;
        this.kita = myThingHandler.getKita();
    }

    public void writeData(ChannelUID channelUID, Command command) {

        this.writeData(channelUID.getId(), command, new ModbusWriteCallback() {

            @Override
            public void onError(ModbusWriteRequestBlueprint request, Exception error) {
                logger.error("Write FAILED Command: {} \n\t Request: {} \n\t ERROR: {}", command, request, error);

                myThingHandler.updateThingStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        String.format("Error (%s) with read. Request: %s. Description: %s. Message: %s",
                                error.getClass().getSimpleName(), request, error.toString(), error.getMessage()));

            }

            @Override
            public void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response) {
                // TODO Auto-generated method stub
                logger.debug("Write OK Command: {} \n\t Request: {} \n\t Response: {}", command, request, response);

                myThingHandler.updateThingStatus(ThingStatus.ONLINE, null, null);

                if (command instanceof State) {
                    myThingHandler.tryUpdateChannelState(channelUID, ((State) command));
                }

            }

        });

    }

    private void writeData(String kitaDataId, Command originalCommand, ModbusWriteCallback modbusWriteCallback) {
        MyKitaHeatPumpConfiguration config = this.myThingHandler.getConfiguration();

        if (config != null && this.myThingHandler.getModbusHandler() != null) {
            KitaHeatPumpDataType dataType = kita.getDataType(kitaDataId);
            if (dataType != null) {

                Command command = this.convertCommand(dataType, originalCommand);

                ModbusWriteRequestBlueprint request = this.requestFromCommand(dataType.register, dataType.address,
                        command, config);

                if (request != null) {

                    ModbusSlaveEndpoint slaveEndpoint = this.myThingHandler.getModbusHandler().getSlaveEndpoint();

                    ModbusManager manager = this.myThingHandler.getModbusHandler().getManager();
                    // if (request == null || slaveEndpoint == null || manager == null) {
                    // return;
                    // }

                    BasicWriteTask writeTask = new BasicWriteTask(slaveEndpoint, request, modbusWriteCallback);

                    logger.trace("Submitting write task: {}", writeTask);
                    manager.submitOneTimeWrite(writeTask);
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
                request = new BasicModbusWriteCoilRequestBlueprint(slaveId, writeStart, data, false, maxTries);
            }

        } else // if (dataType.register == RegisterTypeEnum.holding)
        {
            ModbusConstants.ValueType writeValueType = ModbusConstants.ValueType.INT16;
            ModbusRegisterArray data = ModbusBitUtilities.commandToRegisters(command, writeValueType);

            boolean writeMultiple = data.size() > 1;
            request = new BasicModbusWriteRegisterRequestBlueprint(slaveId, writeStart, data, writeMultiple, maxTries);
        }

        return request;
    }

    private Command convertCommand(KitaHeatPumpDataType dataType, Command originalCommand) {

        double factor = this.writeFactor(dataType);

        if (originalCommand instanceof QuantityType) {
            DecimalType convertedCommand = new DecimalType(((QuantityType) originalCommand).doubleValue() * factor);
            return convertedCommand;
        } else if (originalCommand instanceof DecimalType && factor != 1) {
            DecimalType convertedCommand = new DecimalType(((DecimalType) originalCommand).doubleValue() * factor);
            return convertedCommand;
        }
        return originalCommand;
    }

    private double writeFactor(KitaHeatPumpDataType dataType) {
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

}
