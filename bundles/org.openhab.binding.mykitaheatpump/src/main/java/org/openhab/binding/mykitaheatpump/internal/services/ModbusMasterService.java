package org.openhab.binding.mykitaheatpump.internal.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mykitaheatpump.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusMasterService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final KitaHeatPump kita;
    final MyKitaHeatPumpThingHandler myThingHandler;

    final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();

    public ModbusMasterService(KitaHeatPump kita, MyKitaHeatPumpThingHandler myThingHandler) {

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
            int startAddress = address;
            int length = 1;

            DataValuePoller poller = new DataValuePoller(myThingHandler, fnCode, startAddress, length);
            pollers.add(poller);
        });

        pollers.forEach(poller -> {
            poller.registerPollTask();
        });
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

    public void dispose() {

        pollers.forEach(poller -> {

            poller.unregisterPollTask();
        });
        pollers.clear();

    }

}
