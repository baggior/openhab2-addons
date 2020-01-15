package org.openhab.binding.mykitaheatpump.internal.modbus;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.DataTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;

public class ModbusConversionUtils {

    static ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
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

    static ValueType convertToValueType(DataTypeEnum type) {
        switch (type) {
            case _bool:
                return ValueType.INT16;
            case _switch:
                return ValueType.INT16;
            case dateTime:
                return ValueType.INT16; // TODO

            case number:
            case pct:
            case cop:
            case flow_ro:
            case temperature_ro:
            case temperature_rw:
            case rps_ro:
                return ValueType.INT16;

            case string:
                throw new RuntimeException("invalid DataTypeEnum string!");
            default:
                throw new RuntimeException("DataTypeEnum unknown! -> " + type.toString());

        }
    }

    static DecimalType convertState(KitaHeatPumpDataType dataType, DecimalType originalState) {
        double factor = readFactor(dataType);

        DecimalType convertedState = new DecimalType(originalState.doubleValue() * factor);
        return convertedState;
    }

    static double readFactor(KitaHeatPumpDataType dataType) {
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
                    return 0.1;
            }
        }
        return 1;
    }

}
