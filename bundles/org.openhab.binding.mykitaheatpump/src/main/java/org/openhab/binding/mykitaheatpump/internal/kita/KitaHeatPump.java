package org.openhab.binding.mykitaheatpump.internal.kita;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.DataTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.RegisterTypeEnum;

@NonNullByDefault
public class KitaHeatPump {

    public static enum ModelsEnum {
        Splus
    }

    ModelsEnum model = ModelsEnum.Splus;
    String version = "1.0";
    String description = "data 2017";

    final HashMap<KitaHeatPumpDataItem, KitaHeatPumpDataValue> dataStore;

    public KitaHeatPump() {
        this.dataStore = new HashMap<KitaHeatPumpDataItem, KitaHeatPumpDataValue>();

        this.build();
    }

    private void build() {
        this.dataStore.clear();

        KitaHeatPumpDataItem dataType = new KitaHeatPumpDataItem("b1", "B1: subcooling temperature in HP mode",
                DataTypeEnum.temperature_ro, 1, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b3", "B3: sanitary hot water puffer temperature",
                DataTypeEnum.temperature_ro, 3, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b4", "B4: inlet water temperature", DataTypeEnum.temperature_ro, 4,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("b5", "B5: waterflow (if flowmeter fitted)", DataTypeEnum.flow_ro, 5,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("b7", "B7: outlet water temperature", DataTypeEnum.temperature_ro, 7,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b8", "B8: external air temperature", DataTypeEnum.temperature_ro, 8,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b9", "B9: compressor discharge temperature", DataTypeEnum.temperature_ro,
                9, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b10", "B10: compressor suction temperature", DataTypeEnum.temperature_ro,
                10, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("b12c", "Evaporator temperature from Low pressure conversion",
                DataTypeEnum.temperature_ro, 13, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("b11c", "Condensing temperature from High pressure conversion",
                DataTypeEnum.temperature_ro, 14, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("superheating", "Superheating", DataTypeEnum.temperature_ro, 15,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("rps", "Compressor rotor speed", DataTypeEnum.rps_ro, 18,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("avg_ext", "Average external temperature", DataTypeEnum.temperature_ro, 31,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("opt_delta", "Delta optimizer", DataTypeEnum.temperature_ro, 32,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        // SET POINTS
        dataType = new KitaHeatPumpDataItem("chiller_set", "Setpoint chiller mode (flash memory)",
                DataTypeEnum.temperature_rw, 19, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("plant_set", "Setpoint heat pump mode (flash memory)",
                DataTypeEnum.temperature_rw, 20, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("acs_set", "Setpoint sanitary water (flash memory)",
                DataTypeEnum.temperature_rw, 21, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("ram_chiller_set", "Setpoint chiller mode temperature (ram memory)",
                DataTypeEnum.temperature_rw, 65, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("ram_plant_set", "Setpoint heat pump temperature (ram memory)",
                DataTypeEnum.temperature_rw, 66, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("ram_acs_set", "Setpoint sanitary temperature (ram memory)",
                DataTypeEnum.temperature_rw, 67, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        // STATE
        dataType = new KitaHeatPumpDataItem("active_set", "Setpoint Active", DataTypeEnum.temperature_ro, 68,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataItem("vA_pct", "Opening percentage valve A", DataTypeEnum.pct, 70,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        // COP
        dataType = new KitaHeatPumpDataItem("cop", "Measure of COP", DataTypeEnum.cop, 76, RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

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

        // INTEGERs
        dataType = new KitaHeatPumpDataItem("switch", "Switch State", DataTypeEnum._switch, 221,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("mode", "Operation Mode", DataTypeEnum._switch, 227,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());

        // TODO
        dataType = new KitaHeatPumpDataItem("cmp_inst_pct", "Istant compressor power percentage", DataTypeEnum.pct, 242,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
        dataType = new KitaHeatPumpDataItem("cmp_avg_pct", "Average compressor power percentage", DataTypeEnum.pct, 243,
                RegisterTypeEnum.holding);
        dataStore.put(dataType, new KitaHeatPumpDataValue());
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

    public HashMap<KitaHeatPumpDataItem, KitaHeatPumpDataValue> getDataStore() {
        return dataStore;
    }

    public KitaHeatPumpDataItem getDataType(String dataName) {
        return this.dataStore.keySet().stream().filter((KitaHeatPumpDataItem item) -> {
            return item != null && item.name.equals(dataName);
        }).findAny().orElse(null);
    }

}
