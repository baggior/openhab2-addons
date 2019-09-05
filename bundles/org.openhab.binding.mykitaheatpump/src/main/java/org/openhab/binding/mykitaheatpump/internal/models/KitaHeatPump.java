package org.openhab.binding.mykitaheatpump.internal.models;

import java.util.HashMap;

import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.DataTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.RegisterTypeEnum;

public class KitaHeatPump {

    public static enum ModelsEnum {
        Splus
    }

    ModelsEnum model = ModelsEnum.Splus;
    String version = "1.0";
    String description = "data 2017";

    final HashMap<KitaHeatPumpDataType, KitaHeatPumpDataValue> data;

    public KitaHeatPump() {
        this.data = new HashMap<KitaHeatPumpDataType, KitaHeatPumpDataValue>();

        this.build();
    }

    private void build() {
        this.data.clear();

        KitaHeatPumpDataType dataType = new KitaHeatPumpDataType("b1", "B1: subcooling temperature in HP mode [°C]",
                DataTypeEnum.temperature_ro, 1, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("b3", "B3: sanitary hot water puffer temperature [°C]",
                DataTypeEnum.temperature_ro, 3, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("b4", "B4: inlet water temperature [°C]", DataTypeEnum.temperature_ro, 4,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("b5", "B5: waterflow (if flowmeter fitted) [l/m]", DataTypeEnum.flow_ro, 5,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("b7", "B7: outlet water temperature [°C]", DataTypeEnum.temperature_ro, 7,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("b8", "B8: external air temperature [°C]", DataTypeEnum.temperature_ro, 8,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("b9", "B9: compressor discharge temperature [°C]",
                DataTypeEnum.temperature_ro, 9, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("b10", "B10: compressor suction temperature [°C]",
                DataTypeEnum.temperature_ro, 10, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        // TODO

        // SET POINTS
        dataType = new KitaHeatPumpDataType("chiller_set", "Setpoint chiller mode (flash memory) [°C]",
                DataTypeEnum.temperature_rw, 19, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("plant_set", "Setpoint heat pump mode (flash memory) [°C]",
                DataTypeEnum.temperature_rw, 20, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("acs_set", "Setpoint sanitary water (flash memory) [°C]",
                DataTypeEnum.temperature_rw, 20, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("ram_chiller_set", "Setpoint chiller mode temperature (ram memory) [°C]",
                DataTypeEnum.temperature_rw, 65, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("ram_plant_set", "Setpoint heat pump temperature (ram memory) [°C]",
                DataTypeEnum.temperature_rw, 66, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataType("ram_acs_set", "Setpoint sanitary temperature (ram memory) [°C]",
                DataTypeEnum.temperature_rw, 67, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        // STATE
        dataType = new KitaHeatPumpDataType("active_set", "Setpoint Active [°C]", DataTypeEnum.temperature_ro, 68,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("vA_pct", "Opening percentage valve A", DataTypeEnum.pct, 70,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        // COP
        dataType = new KitaHeatPumpDataType("cop", "Measure of COP", DataTypeEnum.cop, 76, RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        // dataType = new KitaHeatPumpDataType("acs", 130);
        // data.put(dataType, new KitaHeatPumpDataValue());
        //
        // dataType = new KitaHeatPumpDataType("power", 172);
        // data.put(dataType, new KitaHeatPumpDataValue());
        //
        // dataType = new KitaHeatPumpDataType("onOffState", "onOffState", DataTypeEnum._switch, 50,
        // RegisterTypeEnum.holding);
        // data.put(dataType, new KitaHeatPumpDataValue());
        //
        // dataType = new KitaHeatPumpDataType("contact", "contact", DataTypeEnum._switch, 40, RegisterTypeEnum.coil);
        // data.put(dataType, new KitaHeatPumpDataValue());

    }

    @Override
    public String toString() {
        return "KitaHeatPump [model=" + model + ", version=" + version + ", description=" + description + "]";
    }

    public ModelsEnum getModel() {
        return model;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public HashMap<KitaHeatPumpDataType, KitaHeatPumpDataValue> getData() {
        return data;
    }

}
