package org.openhab.binding.mykitaheatpump.internal.models;

public class KitaHeatPump {

    public static enum ModelsEnum {
        Splus
    }

    ModelsEnum model;
    String version;
    String description;

    @Override
    public String toString() {
        return "KitaHeatPump [model=" + model + ", version=" + version + ", description=" + description + "]";
    }

}
