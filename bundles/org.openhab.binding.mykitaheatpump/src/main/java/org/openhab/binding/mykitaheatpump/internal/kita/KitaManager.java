package org.openhab.binding.mykitaheatpump.internal.kita;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.handler.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.modbus.ModbusPollers;
import org.openhab.binding.mykitaheatpump.internal.modbus.ModbusWriter;
import org.openhab.core.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.core.thing.ThingStatus;

@NonNullByDefault
public class KitaManager {

    private final MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler;
    private final ModbusCommunicationInterface modbusInterface;
    private MyKitaHeatPumpConfiguration configuration;

    private final KitaHeatPump kita;
    private final ModbusPollers modbusPollers;
    private final ModbusWriter modbusWriter;

    public KitaManager(MyKitaHeatPumpThingHandler kitaHeatPumpThingHandler,
            ModbusCommunicationInterface modbusInterface, MyKitaHeatPumpConfiguration configuration) {
        this.kitaHeatPumpThingHandler = kitaHeatPumpThingHandler;
        this.modbusInterface = modbusInterface;
        this.configuration = configuration;

        this.kita = new KitaHeatPump();
        this.modbusPollers = new ModbusPollers(this);
        this.modbusWriter = new ModbusWriter(this);
    }

    public void startup() {
        this.buildChannels();
        this.startPoolers();

    }

    public void shutdown() {

    }

    public MyKitaHeatPumpThingHandler getKitaHeatPumpThingHandler() {
        return kitaHeatPumpThingHandler;
    }

    public KitaHeatPump getKita() {
        return kita;
    }

    /**
     * Gets the {@link ModbusCommunicationInterface} represented by the thing
     *
     * Note that this can be <code>null</code> in case of incomplete initialization
     *
     * @return communication interface represented by this thing handler
     */
    public ModbusCommunicationInterface getCommunicationInterface() {
        return this.modbusInterface;
    }

    public MyKitaHeatPumpConfiguration getConfiguration() {
        return this.configuration;
    }

    public boolean isActive() {
        return (kitaHeatPumpThingHandler.getStatusInfo().getStatus() == ThingStatus.ONLINE
                && !kitaHeatPumpThingHandler.isDisposed());
    }

    private void buildChannels() {
        if (!kitaHeatPumpThingHandler.isDisposed()) {
            ChannelsBuilder.of(this.getKita(), kitaHeatPumpThingHandler.getChannelsHandler()).build();
        }
    }

    private void startPoolers() {
        if (!kitaHeatPumpThingHandler.isDisposed()) {
            this.modbusPollers.initializePollers();
        }
    }

}
