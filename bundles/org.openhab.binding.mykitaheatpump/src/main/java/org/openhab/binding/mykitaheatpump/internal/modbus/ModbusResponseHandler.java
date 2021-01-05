package org.openhab.binding.mykitaheatpump.internal.modbus;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.modbus.BitArray;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;

@NonNullByDefault
public interface ModbusResponseHandler {

    public void processUpdateStates(ModbusReadRequestBlueprint request, ModbusRegisterArray registers);

    public void processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray);

    public void processOnReadError(ModbusReadRequestBlueprint request, Exception cause);
}