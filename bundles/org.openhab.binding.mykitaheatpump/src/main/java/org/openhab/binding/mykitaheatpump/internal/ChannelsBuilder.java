package org.openhab.binding.mykitaheatpump.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.DataTypeEnum;

@NonNullByDefault
public class ChannelsBuilder {
    final KitaHeatPump kita;
    // final String thingUid;
    final ChannelsHandler channelsHandler;

    ChannelsBuilder(KitaHeatPump kita, ChannelsHandler channelsHandler) {
        this.kita = kita;
        this.channelsHandler = channelsHandler;
        // this.thingUid = channelsHandler.thingHandler.getUID().getAsString();
    }

    public static ChannelsBuilder create(KitaHeatPump kita, ChannelsHandler channelsHandler) {
        ChannelsBuilder builder = new ChannelsBuilder(kita, channelsHandler);
        return builder;
    }

    List<Channel> build() {
        List<Channel> ret = new ArrayList<Channel>();

        this.kita.getData().forEach((dataType, dataValue) -> {
            // todo
            String id = dataType.name;
            String label = dataType.label;

            ChannelUID channelUID = channelsHandler.getChannelUID(id);
            String acceptedItemType = this.convertToItemType(dataType.type);

            ChannelBuilder builder = ChannelBuilder.create(channelUID, acceptedItemType).withLabel(label);

            ChannelTypeUID ctypeUUID = this.convertToChannelType(dataType.type);
            if (ctypeUUID != null) {
                builder.withType(ctypeUUID);
            }

            ChannelKind cKind = this.convertToChannelKind(dataType.type);
            if (cKind != null) {
                builder.withKind(cKind);
            }

            Channel ch = builder.build();
            ret.add(ch);
        });

        return ret;
    }

    private @Nullable ChannelKind convertToChannelKind(DataTypeEnum type) {
        // TODO Auto-generated method stub
        return null;
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
