package org.openhab.binding.mythsensors.internal.modbus;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.mythsensors.internal.MyTHSensorsHandler;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusPollers {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final MyTHSensorsHandler myThingHandler;
    final ModbusMasterService modbusMasterService;
    final ModbusSlaveEndpoint modbusSlaveEndpoint;

    // final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();
    final Map<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller> pollers = new HashMap<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller>();

    int cacheMillis = 50; // TODO

    int maxTries = 1; // TODO
    int port = 502; // TODO
    String host = "localhost"; // TODO

    private final ReadCallback readCallback = new ReadCallback(this);

    public ModbusPollers(MyTHSensorsHandler myThingHandler, @Reference ModbusMasterService modbusMasterService) {

        this.myThingHandler = myThingHandler;
        this.modbusMasterService = modbusMasterService;

        this.modbusSlaveEndpoint = new ModbusTCPSlaveEndpoint(host, port);
    }

    public ModbusSlaveEndpoint getSlaveEndpoint() {
        return this.modbusSlaveEndpoint;
    }

    public void initializePollers() {
        logger.debug("ModbusMasterService initialize pollers() ");

        // kita.getData().forEach((dataType, dataValue) -> {
        // // todo
        //
        // RegisterTypeEnum register = dataType.register;
        // int address = dataType.address;
        //
        // ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(register);
        // DataValuePoller poller = pollers.get(fnCode);
        // if (poller == null) {
        // int startAddress = address;
        // int length = 1;
        // poller = new DataValuePoller(this, fnCode, startAddress, length);
        //
        // } else {
        // int startAddress = address;
        // int oldEndAddress = poller.start + poller.length;
        // int newEndAddress = startAddress + 1;
        // if (startAddress < poller.start) {
        // poller.start = startAddress;
        // poller.length = oldEndAddress - startAddress;
        // } else if (newEndAddress > oldEndAddress) {
        // poller.length = newEndAddress - poller.start;
        // }
        //
        // if (poller.length > 123) {
        // throw new RuntimeException("poller.length troppo grande " + poller.length + " > 123 !");
        // }
        //
        // }
        // pollers.put(fnCode, poller);
        // });

        pollers.values().forEach((poller) -> {
            if (poller != null) {
                poller.registerPollTask();
            }
        });
    }

    // private ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
    // switch (register) {
    // case coil:
    // return ModbusReadFunctionCode.READ_COILS;
    // case holding:
    // return ModbusReadFunctionCode.READ_INPUT_REGISTERS;
    //
    // default:
    // throw new RuntimeException("RegisterTypeEnum unknown!");
    // }
    // }

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

        // this.kita.getData().forEach((type, value) -> {
        //
        // ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
        // if (!fnCode.equals(request.getFunctionCode())) {
        // return;
        // }
        //
        // int readIndex = type.address;
        // int pollStart = request.getReference();
        // int extractIndex = readIndex - pollStart;
        //
        // ModbusConstants.ValueType readValueType = this.convertToValueType(type.type);
        //
        // State numericState = ModbusBitUtilities.extractStateFromRegisters(registers, extractIndex, readValueType)
        // .map(state -> (State) state).orElse(UnDefType.UNDEF);
        //
        // boolean boolValue = !numericState.equals(DecimalType.ZERO);
        //
        // String label = type.name;
        // ChannelUID channelUID = this.myThingHandler.getChannelsHandler().getChannelUID(label);
        // ret.put(channelUID, numericState);
        //
        // });

        return ret;
    }

    public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray) {
        Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();

        // this.kita.getData().forEach((type, value) -> {
        //
        // ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
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

        return ret;
    }
    //
    // private ValueType convertToValueType(DataTypeEnum type) {
    // switch (type) {
    // case _bool:
    // return ValueType.INT16;
    // case _switch:
    // return ValueType.INT16;
    // case dateTime:
    // return ValueType.INT16;
    // case number:
    // return ValueType.INT16;
    // case string:
    // throw new RuntimeException("invalid DataTypeEnum string!");
    // default:
    // throw new RuntimeException("DataTypeEnum unknown!");
    //
    // }
    //
    // }

}
