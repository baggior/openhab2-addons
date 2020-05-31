package org.openhab.binding.mykitaheatpump.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages OpenHab Thing Channels and cache
 * 
 * @author utente
 *
 */
@NonNullByDefault
public class ChannelsHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, ChannelUID> channelCache = new HashMap<>();
    private Map<String, ChannelTypeUID> channelTypeCache = new HashMap<>();
    private Map<ChannelUID, State> channelLastState = new HashMap<>();
    private Map<ChannelUID, Long> channelLastUpdated = new HashMap<>();
    private long updateUnchangedValuesEveryMillis;

    final MyKitaHeatPumpHandler thingHandler;

    ChannelsHandler(MyKitaHeatPumpHandler myKitaHeatPumpHandler) {
        this.thingHandler = myKitaHeatPumpHandler;
    }

    void configure(MyKitaHeatPumpConfiguration configuration) {
        logger.debug("ChannelsHandler configuration..");
        updateUnchangedValuesEveryMillis = configuration.updateUnchangedValuesEveryMillis;

    }

    public ChannelUID getChannelUID(String channelID) {
        return channelCache.computeIfAbsent(channelID, id -> new ChannelUID(thingHandler.getUID(), id));
    }

    public ChannelTypeUID getChannelTypeUID(String channelTypeID) {
        return channelTypeCache.computeIfAbsent(channelTypeID,
                id -> new ChannelTypeUID(MyKitaHeatPumpBindingConstants.BINDING_ID, id));
    }

    public void updateExpiredChannels(Map<ChannelUID, State> states) {
        synchronized (this) {
            // updateStatusIfChanged(ThingStatus.ONLINE);
            long now = System.currentTimeMillis();
            // Update channels that have not been updated in a while, or when their values has changed
            states.forEach((uid, state) -> updateExpiredChannel(now, uid, state));
            channelLastState = states;
        }
    }

    private void updateExpiredChannel(long now, ChannelUID uid, State state) {
        @Nullable
        State lastState = channelLastState.get(uid);
        long lastUpdatedMillis = channelLastUpdated.getOrDefault(uid, 0L);
        long millisSinceLastUpdate = now - lastUpdatedMillis;
        if (lastUpdatedMillis <= 0L || lastState == null || updateUnchangedValuesEveryMillis <= 0L
                || millisSinceLastUpdate > updateUnchangedValuesEveryMillis || !lastState.equals(state)) {

            thingHandler.tryUpdateChannelState(uid, state);

            channelLastUpdated.put(uid, now);
        }
    }

    void dispose() {

        logger.debug("ChannelsHandler dispose..");
        channelCache.clear();
        channelLastState.clear();
        channelLastUpdated.clear();
    }

}
