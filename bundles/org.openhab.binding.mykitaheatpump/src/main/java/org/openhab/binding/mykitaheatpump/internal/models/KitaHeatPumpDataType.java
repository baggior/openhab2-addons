package org.openhab.binding.mykitaheatpump.internal.models;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class KitaHeatPumpDataType {

    public static enum DataTypeEnum {
        string,
        dateTime,
        number,
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
    public String description;

    public RegisterTypeEnum register;
    public int address;

    KitaHeatPumpDataType(String name, int address) {
        this(name, name, DataTypeEnum.number, address, RegisterTypeEnum.holding);
    }

    KitaHeatPumpDataType(String name, String description, DataTypeEnum datatype, int address,
            RegisterTypeEnum registerType) {
        readonly = false;

        type = datatype;
        this.name = name;
        this.description = description;
        this.register = registerType;
        this.address = address;
    }

}
