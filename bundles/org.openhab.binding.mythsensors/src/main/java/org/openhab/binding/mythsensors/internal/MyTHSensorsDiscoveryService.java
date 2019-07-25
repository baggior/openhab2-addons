package org.openhab.binding.mythsensors.internal;

import static org.openhab.binding.mythsensors.internal.MyTHSensorsBindingConstants.THING_TYPE_THSENSOR;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "discovery.mythsensors", service = DiscoveryService.class, immediate = true)
public class MyTHSensorsDiscoveryService extends AbstractDiscoveryService {

    public static final String DEFAULT_THING_ID = "unknown";
    public static final String DEFAULT_THING_LABEL_PREFIX = "TH Sensor";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_THSENSOR);
    private static final int DISCOVERY_TIMEOUT_SECONDS = 10;

    public MyTHSensorsDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SECONDS);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting MyTHSensorsDiscoveryService discovery !");

        // TODO Auto-generated method stub

        // found
        final String thingId = DEFAULT_THING_ID;

        ThingUID thingUID = new ThingUID(THING_TYPE_THSENSOR, thingId);
        super.thingDiscovered(
                DiscoveryResultBuilder.create(thingUID).withLabel(DEFAULT_THING_LABEL_PREFIX + " - " + thingId)
                        .withRepresentationProperty(DEFAULT_THING_ID).build());

        this.stopScan();
        logger.debug("End MyTHSensorsDiscoveryService discovery.");
    }

    // @Override
    // protected void activate(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
    // // TODO Auto-generated method stub
    // logger.debug("DiscoveryService.activate configProperties \n\t {}", configProperties);
    // super.activate(configProperties);
    // }

}
