package org.openhab.binding.mythsensors.internal.discovery;

import static org.openhab.binding.mythsensors.internal.MyTHSensorsBindingConstants.THING_TYPE_THSENSOR;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "binding.mythsensors", service = { DiscoveryService.class,
        MyTHSensorsDiscoveryService.class })
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
        logger.debug("Start MyTHSensorsDiscoveryService discovery !");

        // found
        final String thingId = DEFAULT_THING_ID;

        ThingUID thingUID = new ThingUID(THING_TYPE_THSENSOR, thingId);
        super.thingDiscovered(
                DiscoveryResultBuilder.create(thingUID).withLabel(DEFAULT_THING_LABEL_PREFIX + " - " + thingId)
                        .withRepresentationProperty(DEFAULT_THING_ID).build());

        this.stopScan();
        logger.debug("End MyTHSensorsDiscoveryService discovery.");
    }

    @Override
    @Activate
    protected void activate(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        // TODO Auto-generated method stub
        logger.debug("MyTHSensorsDiscoveryService.activate configProperties \n\t {}", configProperties);

        if (configProperties != null) {
            String host = (String) configProperties.get("host");
            logger.debug("MyTHSensorsDiscoveryService.activate host: {}", host);
        }

        super.activate(configProperties); // starts background discovery
    }

    @Override
    @Modified
    protected void modified(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        // TODO Auto-generated method stub
        super.modified(configProperties);

        logger.debug("MyTHSensorsDiscoveryService.modified configProperties \n\t {}", configProperties);
    }

}
