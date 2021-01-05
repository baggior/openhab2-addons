package org.openhab.binding.mykitaheatpump.internal.kita;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.handler.ChannelsHandler;
import org.openhab.binding.mykitaheatpump.internal.handler.ChannelsHandler.BuildChannelConfig;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.DataTypeEnum;
import org.openhab.core.thing.type.ChannelTypeUID;

@NonNullByDefault
class ChannelsBuilder {
    @Nullable
    private KitaHeatPump kita;
    private final ChannelsHandler channelsHandler;

    private ChannelsBuilder(ChannelsHandler channelsHandler) {
        this.channelsHandler = channelsHandler;
        // this.thingUid = channelsHandler.thingHandler.getUID().getAsString();
    }

    public static ChannelsBuilder of(KitaHeatPump kita, ChannelsHandler channelsHandler) {
        ChannelsBuilder builder = new ChannelsBuilder(channelsHandler);
        builder.kita(kita);

        return builder;
    }

    public ChannelsBuilder kita(KitaHeatPump kita) {
        this.kita = kita;
        return this;
    }

    public void build() {

        var buildChConfig = new ArrayList<BuildChannelConfig>();
        if (this.kita != null) {

            this.kita.getDataStore().forEach((dataType, dataValue) -> {
                // todo
                String id = dataType.name;
                String label = dataType.label;
                DataTypeEnum type = dataType.type;

                var buildChConfigItem = this.channelConfig(id, label, type);
                if (buildChConfigItem != null) {
                    buildChConfig.add(buildChConfigItem);
                }
            });

        }
        channelsHandler.rebuildChannels(buildChConfig);

    }

    private @Nullable BuildChannelConfig channelConfig(String id, String label, DataTypeEnum type) {
        String acceptedItemType = this.convertToItemType(type);
        String channelTypeID = convertToChannelTypeID(type);
        String cKind = this.convertToChannelKind(type);
        if (channelTypeID != null && cKind != null) {
            return new BuildChannelConfig(id, acceptedItemType, label, channelTypeID, cKind);
        }
        return null;
    }

    private @Nullable String convertToChannelKind(DataTypeEnum type) {
        // TODO Auto-generated method stub
        // ChannelKind.TRIGGER;
        return "STATE";
    }

    private @Nullable String convertToChannelTypeID(DataTypeEnum type) {
        String ret = null;
        switch (type) {
            case _bool:
                break;
            case _switch:
                break;
            case dateTime:
                break;
            case number:
                break;
            case string:
                break;
            case temperature_ro:
                ret = ("temperature-ro-ctype");
                break;
            case temperature_rw:
                ret = ("temperature-rw-ctype");
                break;
            case flow_ro:
                ret = ("flow-ro-ctype");
                break;
            case pct:
                ret = ("pct-ctype");
                break;
            case cop:
                ret = ("cop-ctype");
                break;
            case rps_ro:
                ret = ("rps-ro-ctype");
                break;
            default:
                break;
        }

        return ret;
    }

    private @Nullable ChannelTypeUID convertToChannelType(DataTypeEnum type) {
        // TODO Auto-generated method stub
        ChannelTypeUID ctypeUUID = null;
        switch (type) {
            case _bool:
                break;
            case _switch:
                break;
            case dateTime:
                break;
            case number:
                break;
            case string:
                break;
            case temperature_ro:
                ctypeUUID = channelsHandler.getChannelTypeUID("temperature-ro-ctype");
                break;
            case temperature_rw:
                ctypeUUID = channelsHandler.getChannelTypeUID("temperature-rw-ctype");
                break;
            case flow_ro:
                ctypeUUID = channelsHandler.getChannelTypeUID("flow-ro-ctype");
                break;
            case pct:
                ctypeUUID = channelsHandler.getChannelTypeUID("pct-ctype");
                break;
            case cop:
                ctypeUUID = channelsHandler.getChannelTypeUID("cop-ctype");
                break;
            case rps_ro:
                ctypeUUID = channelsHandler.getChannelTypeUID("rps-ro-ctype");
                break;
            default:
                break;

        }

        return ctypeUUID;
    }

    private String convertToItemType(DataTypeEnum type) {
        switch (type) {
            case dateTime:
                return "DateTime";

            case number:
                return "Number";

            case temperature_ro:
            case temperature_rw:
                return "Number:Temperature";

            case pct:
            case cop:
            case rps_ro:
                return "Number:Dimensionless";

            case flow_ro:
                return "Number:VolumetricFlowRate";

            case _switch:
            case _bool:
                return "Switch";

            case string:
                return "String";

            default:
                throw new RuntimeException("DataTypeEnum unknown");
        }
    }

}
