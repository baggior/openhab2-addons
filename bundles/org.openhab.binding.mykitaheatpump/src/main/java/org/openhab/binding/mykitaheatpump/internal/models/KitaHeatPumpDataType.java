package org.openhab.binding.mykitaheatpump.internal.models;

public class KitaHeatPumpDataType {

    public static enum TypeEnum {
        string,
        number
    }

    public static enum RegisterEnum {
        coil,
        holding
    }

    public TypeEnum type;
    public boolean readonly;

    public String name;
    public String description;

    public RegisterEnum register;
    public int address;
}
