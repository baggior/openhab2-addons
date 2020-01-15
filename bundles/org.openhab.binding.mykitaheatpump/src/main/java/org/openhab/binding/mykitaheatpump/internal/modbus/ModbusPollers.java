package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusPollers {

    private static final int MAX_POLLER_LENGTH = 120;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final MyKitaHeatPumpThingHandler myThingHandler;

    // final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();
    final List<ModbusDataValuePoller> pollers = new ArrayList<ModbusDataValuePoller>();

    public ModbusPollers(MyKitaHeatPumpThingHandler myThingHandler) {
        this.myThingHandler = myThingHandler;
    }

    public void initializePollers() {
        logger.debug("ModbusMasterService initialize pollers() ");

        final Map<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller> _tempPollers = new HashMap<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller>();

        KitaHeatPump kita = this.myThingHandler.getKita();
        kita.getData().forEach((dataType, dataValue) -> {
            // Todo

            RegisterTypeEnum register = dataType.register;
            int address = dataType.address;

            ModbusReadFunctionCode fnCode = ModbusConversionUtils.convertToModbusReadFunctionCode(register);
            ModbusDataValuePoller poller = _tempPollers.get(fnCode);
            if (poller == null) {
                int startAddress = address;
                int length = 1;
                poller = new ModbusDataValuePoller(this.myThingHandler, fnCode, startAddress, length);

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

            }
            _tempPollers.put(fnCode, poller);
        });

        // build pollers map
        _tempPollers.values().forEach((poller) -> {
            if (poller != null) {
                if (poller.length > MAX_POLLER_LENGTH) {
                    this.logger.info(
                            "Split poller {} \npoller.length troppo grande: {}>" + MAX_POLLER_LENGTH
                                    + " => Superata la dimenzione massima di 256byte per un pacchetto MODBUS",
                            poller, poller.length);
                    // throw new RuntimeException("poller.length troppo grande " + poller.length
                    // + " > 120! => Superata la dimenzione massima di 256byte per un pacchetto MODBUS");

                    // split poller
                    List<ModbusDataValuePoller> splittedPollers = this.splitPoller(poller, MAX_POLLER_LENGTH);
                    this.logger.debug("Split poller {} to\n {}>", poller, splittedPollers);

                    pollers.addAll(splittedPollers);

                } else {
                    pollers.add(poller);
                }
            }
        });

        // start pollTask
        pollers.forEach((poller) -> {
            poller.registerPollTask();
        });
    }

    private List<ModbusDataValuePoller> splitPoller(final ModbusDataValuePoller poller, final int maxLength) {

        // split poller ricorsivo

        List<ModbusDataValuePoller> ret = new ArrayList<ModbusDataValuePoller>();

        if (poller.length > maxLength) {
            ModbusDataValuePoller firstPoller = new ModbusDataValuePoller(this.myThingHandler, poller.functionCode,
                    poller.start, MAX_POLLER_LENGTH);
            ModbusDataValuePoller nextPoller = new ModbusDataValuePoller(this.myThingHandler, poller.functionCode,
                    poller.start + MAX_POLLER_LENGTH, poller.length - MAX_POLLER_LENGTH);

            ret.add(firstPoller);
            ret.addAll(this.splitPoller(nextPoller, maxLength));
        } else {
            ret.add(poller);
        }

        return ret;

    }

    // private ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
    // switch (register) {
    // case coil:
    // return ModbusReadFunctionCode.READ_COILS;
    // case discrete_input:
    // return ModbusReadFunctionCode.READ_INPUT_DISCRETES;
    // case holding:
    // return ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS;
    // case input:
    // return ModbusReadFunctionCode.READ_INPUT_REGISTERS;
    //
    // default:
    // throw new RuntimeException("RegisterTypeEnum unknown!");
    // }
    // }

    public void dispose() {

        pollers.forEach(poller -> {
            poller.unregisterPollTask();
        });
        pollers.clear();

        this.disposePollers();
    }

    private void disposePollers() {
        this.myThingHandler.getModbusHandler().getManager().getRegisteredRegularPolls().forEach(pollTask -> {
            ModbusReadRequestBlueprint requestBlueprint = pollTask.getRequest();

            logger.info("Unregistering polling from ModbusManager:\n unitId {}, function {}, start {}, length {}",
                    requestBlueprint.getUnitID(), requestBlueprint.getFunctionCode(), requestBlueprint.getReference(),
                    requestBlueprint.getDataLength());
            this.myThingHandler.getModbusHandler().getManager().unregisterRegularPoll(pollTask);
        });

    }

    // public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request,
    // ModbusRegisterArray registers) {
    //
    // Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();
    //
    // KitaHeatPump kita = this.myThingHandler.getKita();
    // kita.getData().forEach((type, value) -> {
    //
    // ModbusReadFunctionCode fnCode = ModbusConversionUtils.convertToModbusReadFunctionCode(type.register);
    // if (!fnCode.equals(request.getFunctionCode())) {
    // return;
    // }
    //
    // int readIndex = type.address;
    // int pollStart = request.getReference();
    // int extractIndex = readIndex - pollStart;
    // int dataLen = request.getDataLength();
    //
    // if (extractIndex > 0 && extractIndex < dataLen) {
    //
    // ModbusConstants.ValueType readValueType = ModbusConversionUtils.convertToValueType(type.type);
    //
    // Optional<DecimalType> numericStateOpt = ModbusBitUtilities.extractStateFromRegisters(registers,
    // extractIndex, readValueType);
    //
    // boolean boolValue = (numericStateOpt.isPresent()) && (!numericStateOpt.get().equals(DecimalType.ZERO));
    //
    // String id = type.name;
    // ChannelUID channelUID = this.myThingHandler.getChannelsHandler().getChannelUID(id);
    // if (numericStateOpt.isPresent()) {
    // DecimalType numericState = numericStateOpt.get();
    // DecimalType numericStateConverted = ModbusConversionUtils.convertState(type, numericState);
    //
    // ret.put(channelUID, numericStateConverted);
    // } else {
    // ret.put(channelUID, UnDefType.UNDEF);
    //
    // }
    // }
    //
    // });
    //
    // this.myThingHandler.getChannelsHandler().updateExpiredChannels(ret);
    // return ret;
    // }

    // private DecimalType convertState(KitaHeatPumpDataType dataType, DecimalType originalState) {
    // double factor = this.readFactor(dataType);
    //
    // DecimalType convertedState = new DecimalType(originalState.doubleValue() * factor);
    // return convertedState;
    // }
    //
    // private double readFactor(KitaHeatPumpDataType dataType) {
    // if (dataType.register == RegisterTypeEnum.holding) {
    // switch (dataType.type) {
    // case _bool:
    // case _switch:
    // return 1;
    //
    // case dateTime:
    // break;
    // case number:
    // break;
    // case string:
    // break;
    //
    // case cop:
    // case flow_ro:
    // case pct:
    // case temperature_ro:
    // case temperature_rw:
    // case rps_ro:
    // return 0.1;
    // }
    // }
    // return 1;
    // }

    // public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray) {
    // Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();
    //
    // KitaHeatPump kita = this.myThingHandler.getKita();
    // kita.getData().forEach((type, value) -> {
    //
    // ModbusReadFunctionCode fnCode = ModbusConversionUtils.convertToModbusReadFunctionCode(type.register);
    // if (!fnCode.equals(request.getFunctionCode())) {
    // return;
    // }
    //
    // int readIndex = type.address;
    // int pollStart = request.getReference();
    // int extractIndex = readIndex - pollStart;
    //
    // boolean boolValue = bitsarray.getBit(extractIndex);
    //
    // DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;
    //
    // String label = type.name;
    // ChannelUID channelUID = this.myThingHandler.getChannelsHandler().getChannelUID(label);
    // ret.put(channelUID, numericState);
    //
    // });
    //
    // this.myThingHandler.getChannelsHandler().updateExpiredChannels(ret);
    //
    // return ret;
    // }

    // private ValueType convertToValueType(DataTypeEnum type) {
    // switch (type) {
    // case _bool:
    // return ValueType.INT16;
    // case _switch:
    // return ValueType.INT16;
    // case dateTime:
    // return ValueType.INT16; // TODO
    //
    // case number:
    // case pct:
    // case cop:
    // case flow_ro:
    // case temperature_ro:
    // case temperature_rw:
    // case rps_ro:
    // return ValueType.INT16;
    //
    // case string:
    // throw new RuntimeException("invalid DataTypeEnum string!");
    // default:
    // throw new RuntimeException("DataTypeEnum unknown! -> " + type.toString());
    //
    // }
    //
    // }

}
