package org.openhab.binding.mythsensors.internal.discovery;

import static org.openhab.binding.mythsensors.internal.MyTHSensorsBindingConstants.THING_TYPE_THSENSOR;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mythsensors.internal.modbus.ModbusMasterService;
import org.openhab.binding.mythsensors.internal.models.ThSensor;
import org.openhab.io.transport.modbus.ModbusManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

    private String host;
    private Integer port;
    private Integer minId;
    private Integer maxId;

    private ModbusManager manager;

    Map<@NonNull String, @Nullable Object> configProperties;

    public MyTHSensorsDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SECONDS);
    }

    @Override
    protected void startScan() {

        if (StringUtils.isNotBlank(host)) {
            logger.debug("Start MyTHSensorsDiscoveryService discovery scan !");

            ModbusMasterService svc = new ModbusMasterService(manager, this.configProperties);

            for (int guess_unitId = minId; guess_unitId <= maxId; guess_unitId++) {

                ThSensor thsensor = svc.performOneTimeThSensorRequest(guess_unitId);

                if (thsensor != null) {
                    int value_read = thsensor.temperature;
                    if (value_read > 0) {

                        // found
                        final String thingIdStr = String.valueOf(guess_unitId);

                        ThingUID thingUID = new ThingUID(THING_TYPE_THSENSOR, thingIdStr);
                        super.thingDiscovered(DiscoveryResultBuilder.create(thingUID)
                                .withLabel(DEFAULT_THING_LABEL_PREFIX + " - " + thingIdStr)
                                .withProperty("unitId", guess_unitId).withRepresentationProperty("unitId").build());
                    }

                }
            }

            logger.debug("End MyTHSensorsDiscoveryService discovery scan.");
        }

        this.stopScan();
    }

    @Override
    @Activate
    protected void activate(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        // TODO Auto-generated method stub
        logger.debug("MyTHSensorsDiscoveryService.activate configProperties \n\t {}", configProperties);

        if (configProperties != null) {
            this.host = (String) configProperties.get("host");
            this.port = Integer.decode((String) configProperties.get("port"));
            this.minId = Integer.decode((String) configProperties.get("minUnitIdToScan"));
            this.maxId = Integer.decode((String) configProperties.get("maxUnitIdToScan"));

            logger.debug("MyTHSensorsDiscoveryService.activate host: {}:{}, id to scan {} - {}", host, port, minId,
                    maxId);
        }

        this.configProperties = configProperties;

        super.activate(configProperties); // starts background discovery
    }

    @Reference
    public void setModbusManager(ModbusManager manager) {
        logger.debug("Setting ModbusManager: {}", manager);
        this.manager = manager;
    }

    public void unsetModbusManager(ModbusManager manager) {
        this.manager = null;
    }
}
