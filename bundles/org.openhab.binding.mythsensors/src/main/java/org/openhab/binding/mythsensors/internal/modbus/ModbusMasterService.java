package org.openhab.binding.mythsensors.internal.modbus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.transport.modbus.BasicModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.BasicPollTaskImpl;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusMasterService {

    public static enum RegisterTypeEnum {
        coil,
        holding
    }

    public static class ModbusOneTimeResponse {
        @Nullable
        public BitArray bitsarray;
        @Nullable
        public ModbusRegisterArray registers;
        @Nullable
        public Exception error;

    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // final MyTHSensorsHandler myThingHandler;
    final ModbusManager modbusManager;
    final ModbusSlaveEndpoint modbusSlaveEndpoint;

    int cacheMillis = 50; // TODO

    int maxTries = 1; // TODO
    int port = 502; // TODO
    String host = "localhost"; // TODO

    Map<String, @Nullable Object> configProperties;

    public ModbusMasterService(ModbusManager modbusManager, Map<String, @Nullable Object> configProperties) {

        this.modbusManager = modbusManager;
        this.configProperties = configProperties;

        this.modbusSlaveEndpoint = new ModbusTCPSlaveEndpoint(host, port);
    }

    public ModbusSlaveEndpoint getSlaveEndpoint() {
        return this.modbusSlaveEndpoint;
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

    public ModbusOneTimeResponse performOneTimeRequest(int slaveUnitId, RegisterTypeEnum type, int start, int length) {

        CountDownLatch callbackCalled = new CountDownLatch(1);

        BasicModbusReadRequestBlueprint request = new BasicModbusReadRequestBlueprint(slaveUnitId,
                this.convertToModbusReadFunctionCode(type), start, length, maxTries);

        final ModbusOneTimeResponse ret = new ModbusOneTimeResponse();

        PollTask task = new BasicPollTaskImpl(this.modbusSlaveEndpoint, request, new ModbusReadCallback() {

            @Override
            public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
                // TODO Auto-generated method stub
                logger.debug(registers.toHexString());
                ret.registers = registers;
                callbackCalled.countDown();
            }

            @Override
            public void onError(ModbusReadRequestBlueprint request, Exception error) {
                // TODO Auto-generated method stub
                logger.debug(error.toString());
                ret.error = error;
                callbackCalled.countDown();
            }

            @Override
            public void onBits(ModbusReadRequestBlueprint request, BitArray bits) {
                // TODO Auto-generated method stub
                logger.debug(bits.toBinaryString());
                ret.bitsarray = bits;
                callbackCalled.countDown();
            }
        });

        Future<?> f = this.modbusManager.submitOneTimePoll(task);

        try {
            callbackCalled.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // f.
        return ret;

    }

    public ModbusManager getModbusManager() {

        return this.modbusManager;
    }

    private ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
        switch (register) {
            case coil:
                return ModbusReadFunctionCode.READ_COILS;
            case holding:
                return ModbusReadFunctionCode.READ_INPUT_REGISTERS;

            default:
                throw new RuntimeException("RegisterTypeEnum unknown!");
        }
    }
}
