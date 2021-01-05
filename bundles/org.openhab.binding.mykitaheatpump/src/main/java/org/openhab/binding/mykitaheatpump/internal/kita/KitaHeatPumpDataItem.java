package org.openhab.binding.mykitaheatpump.internal.kita;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class KitaHeatPumpDataItem {

    public static enum DataTypeEnum {
        string,
        dateTime,
        number,
        pct,
        cop,
        temperature_ro,
        temperature_rw,
        rps_ro,
        flow_ro,
        _bool,
        _switch
    }

    public static enum RegisterTypeEnum {
        coil, // 01 read coil
        discrete_input, // 02 read discrete input
        holding, // 03 read holding registers
        input, // 04 read input registers

    }

    public DataTypeEnum type;
    public boolean readonly;

    public String name;
    public String label;

    public RegisterTypeEnum register;
    public int address;

    KitaHeatPumpDataItem(String name, int address) {
        this(name, name, DataTypeEnum.number, address, RegisterTypeEnum.holding);
    }

    KitaHeatPumpDataItem(String name, String label, DataTypeEnum datatype, int address, RegisterTypeEnum registerType) {
        readonly = false;

        type = datatype;
        this.name = name;
        this.label = label;
        this.register = registerType;
        this.address = address;
    }

}
