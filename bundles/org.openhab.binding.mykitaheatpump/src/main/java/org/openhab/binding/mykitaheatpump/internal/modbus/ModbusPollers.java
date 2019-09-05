package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.DataTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusPollers {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final KitaHeatPump kita;
    final MyKitaHeatPumpThingHandler myThingHandler;

    // final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();
    final Map<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller> pollers = new HashMap<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller>();

    public ModbusPollers(KitaHeatPump kita, MyKitaHeatPumpThingHandler myThingHandler) {

        this.kita = kita;
        this.myThingHandler = myThingHandler;
    }

    public void initializePollers() {
        logger.debug("ModbusMasterService initialize pollers() ");

        kita.getData().forEach((dataType, dataValue) -> {
            // todo

            RegisterTypeEnum register = dataType.register;
            int address = dataType.address;

            ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(register);
            ModbusDataValuePoller poller = pollers.get(fnCode);
            if (poller == null) {
                int startAddress = address;
                int length = 1;
                poller = new ModbusDataValuePoller(this, fnCode, startAddress, length);

            } else {
                int startAddress = address;
                int oldEndAddress = poller.start + poller.length;
                int newEndAddress = startAddress + 1;
                if (startAddress < poller.start) {
                    poller.start = startAddress;
                    poller.length = oldEndAddress - startAddress;
                } else if (newEndAddress > oldEndAddress) {
                    poller.length = newEndAddress - poller.start;
                }

                if (poller.length > 123) {
                    throw new RuntimeException("poller.length troppo grande " + poller.length + " > 123 !");
                }

            }
            pollers.put(fnCode, poller);
        });

        pollers.values().forEach((poller) -> {
            if (poller != null) {
                poller.registerPollTask();
            }
        });
    }

    private ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
        switch (register) {
            case coil:
                return ModbusReadFunctionCode.READ_COILS;
            case discrete_input:
                return ModbusReadFunctionCode.READ_INPUT_DISCRETES;
            case holding:
                return ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS;
            case input:
                return ModbusReadFunctionCode.READ_INPUT_REGISTERS;

            default:
                throw new RuntimeException("RegisterTypeEnum unknown!");
        }
    }

    public void dispose() {

        pollers.values().forEach(poller -> {
            if (poller != null) {
                poller.unregisterPollTask();
            }
        });
        pollers.clear();

    }

    public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request,
            ModbusRegisterArray registers) {

        Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();

        this.kita.getData().forEach((type, value) -> {

            ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
            if (!fnCode.equals(request.getFunctionCode())) {
                return;
            }

            int readIndex = type.address;
            int pollStart = request.getReference();
            int extractIndex = readIndex - pollStart;

            ModbusConstants.ValueType readValueType = this.convertToValueType(type.type);

            State numericState = ModbusBitUtilities.extractStateFromRegisters(registers, extractIndex, readValueType)
                    .map(state -> (State) state).orElse(UnDefType.UNDEF);

            boolean boolValue = !numericState.equals(DecimalType.ZERO);

            String label = type.name;
            ChannelUID channelUID = this.myThingHandler.getChannelsHandler().getChannelUID(label);
            ret.put(channelUID, numericState);

        });

        this.myThingHandler.getChannelsHandler().updateExpiredChannels(ret);
        return ret;
    }

    public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray) {
        Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();

        this.kita.getData().forEach((type, value) -> {

            ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
            if (!fnCode.equals(request.getFunctionCode())) {
                return;
            }

            int readIndex = type.address;
            int pollStart = request.getReference();
            int extractIndex = readIndex - pollStart;

            boolean boolValue = bitsarray.getBit(extractIndex);

            DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;

            String label = type.name;
            ChannelUID channelUID = this.myThingHandler.getChannelsHandler().getChannelUID(label);
            ret.put(channelUID, numericState);

        });

        this.myThingHandler.getChannelsHandler().updateExpiredChannels(ret);

        return ret;
    }

    private ValueType convertToValueType(DataTypeEnum type) {
        switch (type) {
            case _bool:
                return ValueType.INT16;
            case _switch:
                return ValueType.INT16;
            case dateTime:
                return ValueType.INT16;
            case number:
            case pct:
            case cop:
            case flow_ro:
            case temperature_ro:
            case temperature_rw:
                return ValueType.INT16;
            case string:
                throw new RuntimeException("invalid DataTypeEnum string!");
            default:
                throw new RuntimeException("DataTypeEnum unknown! -> " + type.toString());

        }

    }

}
