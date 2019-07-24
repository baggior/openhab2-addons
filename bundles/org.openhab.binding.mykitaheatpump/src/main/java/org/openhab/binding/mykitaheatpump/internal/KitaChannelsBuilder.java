package org.openhab.binding.mykitaheatpump.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPumpDataType.DataTypeEnum;

@NonNullByDefault
public class KitaChannelsBuilder {
    final KitaHeatPump kita;
    // final String thingUid;
    final ChannelsHandler channelsHandler;

    KitaChannelsBuilder(KitaHeatPump kita, ChannelsHandler channelsHandler) {
        this.kita = kita;
        this.channelsHandler = channelsHandler;
        // this.thingUid = channelsHandler.thingHandler.getUID().getAsString();
    }

    public static KitaChannelsBuilder create(KitaHeatPump kita, ChannelsHandler channelsHandler) {
        KitaChannelsBuilder builder = new KitaChannelsBuilder(kita, channelsHandler);
        return builder;
    }

    List<Channel> build() {
        List<Channel> ret = new ArrayList<Channel>();

        this.kita.getData().forEach((dataType, dataValue) -> {
            // todo
            String label = dataType.name;
            String description = dataType.description;

            ChannelUID channelUID = channelsHandler.getChannelUID(label);
            // ChannelUID channelUID = new ChannelUID(thingUid + ":" + label);

            String acceptedItemType = this.convertToItemType(dataType.type);

            Channel ch = ChannelBuilder.create(channelUID, acceptedItemType).withLabel(label)
                    .withKind(ChannelKind.STATE).withDescription(description).build();

            ret.add(ch);
        });

        return ret;
    }

    private String convertToItemType(DataTypeEnum type) {
        switch (type) {
            case dateTime:
                return "DateTime";

            case number:
                return "Number";

            case string:
                return "String";

            case _bool:
                return "Switch";

            case _switch:
                return "Switch";

            default:
                throw new RuntimeException("DataTypeEnum unknown");
        }
    }

}
