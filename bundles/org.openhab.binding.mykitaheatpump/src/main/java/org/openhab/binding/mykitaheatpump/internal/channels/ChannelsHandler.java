package org.openhab.binding.mykitaheatpump.internal.channels;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.mykitaheatpump.internal.ModbusConfigurationException;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
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

    private final MyKitaHeatPumpThingHandler thingHandler;

    public ChannelsHandler(MyKitaHeatPumpThingHandler myKitaHeatPumpHandler) {
        this.thingHandler = myKitaHeatPumpHandler;
    }

    public void configure() throws ModbusConfigurationException {
        logger.debug("ChannelsHandler configuration..");

        MyKitaHeatPumpConfiguration config = this.thingHandler.getConfiguration();
        if (config == null) {
            throw new ModbusConfigurationException("config must be non-null!");
        }
        updateUnchangedValuesEveryMillis = config.updateUnchangedValuesEveryMillis;

    }

    public void initialize() {
        // TODO Auto-generated method stub
        // List<Channel> channels = ChannelsBuilder.of(kita, this).build();
        // Thing newThing = this.editThing().withChannels(channels).build();
        // this.updateThing(newThing);

    }

    public void dispose() {

        logger.debug("ChannelsHandler dispose..");
        channelCache.clear();
        channelLastState.clear();
        channelLastUpdated.clear();
    }

    public ChannelUID getChannelUID(String channelID) {
        return channelCache.computeIfAbsent(channelID, id -> new ChannelUID(thingHandler.getUID(), id));
    }

    ChannelTypeUID getChannelTypeUID(String channelTypeID) {
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

}
