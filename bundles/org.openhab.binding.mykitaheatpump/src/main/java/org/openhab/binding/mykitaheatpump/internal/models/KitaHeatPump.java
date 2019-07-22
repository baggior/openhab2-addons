package org.openhab.binding.mykitaheatpump.internal.models;

<<<<<<< Upstream, based on origin/2.5.x
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

        KitaHeatPumpDataType dataType = new KitaHeatPumpDataType("acs", 130);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("power", 172);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("onOffState", "onOffState", DataTypeEnum._switch, 50,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("contact", "contact", DataTypeEnum._switch, 40, RegisterTypeEnum.coil);
        data.put(dataType, new KitaHeatPumpDataValue());

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
=======
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
>>>>>>> 9b472df new bindings
    }

}
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

        KitaHeatPumpDataType dataType = new KitaHeatPumpDataType("acs", 130);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("power", 172);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("onOffState", "onOffState", DataTypeEnum._switch, 50,
                RegisterTypeEnum.holding);
        data.put(dataType, new KitaHeatPumpDataValue());

        dataType = new KitaHeatPumpDataType("contact", "contact", DataTypeEnum._switch, 40, RegisterTypeEnum.coil);
        data.put(dataType, new KitaHeatPumpDataValue());

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
