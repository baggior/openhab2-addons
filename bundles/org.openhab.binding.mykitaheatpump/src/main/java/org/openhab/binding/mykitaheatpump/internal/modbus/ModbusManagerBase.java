package org.openhab.binding.mykitaheatpump.internal.modbus;

import org.openhab.binding.mykitaheatpump.internal.kita.KitaManager;

abstract class ModbusManagerBase {
    protected final KitaManager kitaManager;

    protected ModbusManagerBase(KitaManager kitaManager) {
        this.kitaManager = kitaManager;
    }
}
