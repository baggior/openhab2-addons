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
    final String thingUid;

    KitaChannelsBuilder(KitaHeatPump kita, String thingUid) {
        this.kita = kita;
        this.thingUid = thingUid;
    }

    public static KitaChannelsBuilder create(KitaHeatPump kita, String thingUid) {
        KitaChannelsBuilder builder = new KitaChannelsBuilder(kita, thingUid);
        return builder;
    }

    List<Channel> build() {
        List<Channel> ret = new ArrayList<Channel>();

        this.kita.getData().forEach((dataType, dataValue) -> {
            // todo
            String label = dataType.name;
            String description = dataType.description;
            ChannelUID channelUID = new ChannelUID(thingUid + ":" + label);

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
                return "Contact";

            case _switch:
                return "Switch";

            default:
                throw new RuntimeException("DataTypeEnum unknown");
        }
    }

}
