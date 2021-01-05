package org.openhab.binding.mykitaheatpump.internal.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.State;

/**
 * Manages OpenHab Thing Channels and cache
 *
 * @author utente
 *
 */
@NonNullByDefault
public class ChannelsHandler {

    public static class BuildChannelConfig {
        public String id;
        public String acceptedItemType;
        public String label;
        public String channelTypeID;
        public String channelKind;

        public BuildChannelConfig(String id, String acceptedItemType, String label, String channelTypeID,
                String channelKind) {
            this.id = id;
            this.acceptedItemType = acceptedItemType;
            this.label = label;
            this.channelTypeID = channelTypeID;
            this.channelKind = channelKind;
        }
    }

    private Map<String, ChannelUID> channelCache = new HashMap<>();
    private Map<String, ChannelTypeUID> channelTypeCache = new HashMap<>();
    private Map<ChannelUID, State> channelsLastState = new HashMap<>();
    private Map<ChannelUID, Long> channelLastUpdated = new HashMap<>();

    private final MyKitaHeatPumpThingHandler thingHandler;

    ChannelsHandler(MyKitaHeatPumpThingHandler myKitaHeatPumpHandler) {
        this.thingHandler = myKitaHeatPumpHandler;
    }

    public ChannelUID getOrCreateChannelUID(String channelID) {
        return channelCache.getOrDefault(channelID, new ChannelUID(thingHandler.getThing().getUID(), channelID));
    }

    public @Nullable ChannelUID getChannelUID(String channelID) {
        return channelCache.get(channelID);
    }

    public @Nullable ChannelTypeUID getChannelTypeUID(String channelTypeID) {
        return channelTypeCache.computeIfAbsent(channelTypeID,
                id -> new ChannelTypeUID(MyKitaHeatPumpBindingConstants.BINDING_ID, id));
    }

    public void rebuildChannels(ArrayList<BuildChannelConfig> buildChConfig) {
        var channels = new ArrayList<Channel>();
        for (BuildChannelConfig buildChConfigItem : buildChConfig) {
            Channel channel = this.buildChannel(buildChConfigItem);
            channels.add(channel);
        }

        thingHandler.editThingChannels(channels);
    }

    public void updateChannelsState(Map<ChannelUID, State> states) {
        var config = thingHandler.getConfiguration();
        if (!thingHandler.isDisposed() && config != null) {
            long updateUnchangedValuesEveryMillis = config.updateUnchangedValuesEveryMillis;

            synchronized (this) {
                // updateStatusIfChanged(ThingStatus.ONLINE);
                long now = System.currentTimeMillis();
                // Update channels that have not been updated in a while, or when their values has changed
                states.forEach(
                        (uid, state) -> this.updateChannelState(now, uid, state, updateUnchangedValuesEveryMillis));
                channelsLastState = states;
            }
        }
    }

    public void updateChannelState(ChannelUID channelUid, State state) {
        var config = thingHandler.getConfiguration();
        if (!thingHandler.isDisposed() && config != null) {
            long updateUnchangedValuesEveryMillis = config.updateUnchangedValuesEveryMillis;

            synchronized (this) {
                // updateStatusIfChanged(ThingStatus.ONLINE);
                long now = System.currentTimeMillis();
                // Update channels that have not been updated in a while, or when their values has changed
                this.updateChannelState(now, channelUid, state, updateUnchangedValuesEveryMillis);
                channelsLastState.put(channelUid, state);
            }
        }
    }

    // Update channels that have not been updated in a while, or when their values has changed
    private void updateChannelState(long now, ChannelUID uid, State state, long updateUnchangedValuesEveryMillis) {
        @Nullable
        State lastState = channelsLastState.get(uid);
        long lastUpdatedMillis = channelLastUpdated.getOrDefault(uid, 0L);
        long millisSinceLastUpdate = now - lastUpdatedMillis;
        if (lastUpdatedMillis <= 0L || lastState == null || updateUnchangedValuesEveryMillis <= 0L
                || millisSinceLastUpdate > updateUnchangedValuesEveryMillis || !lastState.equals(state)) {

            thingHandler.updateChannelState(uid, state);

            channelLastUpdated.put(uid, now);
        }
    }

    private Channel buildChannel(BuildChannelConfig channelConfig) {
        ChannelUID channelUID = this.getOrCreateChannelUID(channelConfig.id);

        String acceptedItemType = channelConfig.acceptedItemType;

        ChannelBuilder builder = ChannelBuilder.create(channelUID, acceptedItemType).withLabel(channelConfig.label);
        ChannelTypeUID ctypeUUID = this.getChannelTypeUID(channelConfig.channelTypeID);
        if (ctypeUUID != null) {
            builder.withType(ctypeUUID);
        }

        ChannelKind cKind = ChannelKind.parse(channelConfig.channelKind);
        if (cKind != null) {
            builder.withKind(cKind);
        }

        Channel ch = builder.build();
        return ch;

    }
}
