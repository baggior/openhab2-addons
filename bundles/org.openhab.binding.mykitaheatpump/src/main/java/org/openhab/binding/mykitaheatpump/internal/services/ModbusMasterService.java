package org.openhab.binding.mykitaheatpump.internal.services;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
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

    // final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();
    final Map<ModbusReadFunctionCode, @Nullable DataValuePoller> pollers = new HashMap<ModbusReadFunctionCode, @Nullable DataValuePoller>();

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
            DataValuePoller poller = pollers.get(fnCode);
            if (poller == null) {
                int startAddress = address;
                int length = 1;
                poller = new DataValuePoller(myThingHandler, fnCode, startAddress, length);

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
            case holding:
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

}
