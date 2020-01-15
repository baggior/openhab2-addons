package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.mykitaheatpump.internal.ModbusConfigurationException;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpConfiguration;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpThingHandler;
import org.openhab.binding.mykitaheatpump.internal.models.KitaHeatPump;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusHandler implements ModbusManagerListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @NonNull
    private final MyKitaHeatPumpThingHandler handler;
    @NonNull
    private final Supplier<ModbusManager> managerRef;

    @NonNull
    private final ModbusPollers modbusPollers;

    private EndpointPoolConfiguration poolConfiguration;
    private ModbusTCPSlaveEndpoint endpoint;

    public ModbusHandler(@NonNull MyKitaHeatPumpThingHandler handler, Supplier<ModbusManager> managerRef) {
        this.handler = handler;
        this.managerRef = managerRef;
        this.modbusPollers = new ModbusPollers(handler);
    }

    public void configure() throws ModbusConfigurationException {
        MyKitaHeatPumpConfiguration config = this.handler.getConfiguration();
        if (config == null) {
            throw new ModbusConfigurationException("config must be non-null!");
        }

        String host = config.host;
        if (host == null) {
            throw new ModbusConfigurationException("host must be non-null!");
        }

        this.endpoint = new ModbusTCPSlaveEndpoint(host, config.port);

        this.poolConfiguration = new EndpointPoolConfiguration();
        poolConfiguration.setConnectMaxTries(config.connectMaxTries);
        poolConfiguration.setConnectTimeoutMillis(config.connectTimeoutMillis);
        poolConfiguration.setInterConnectDelayMillis(config.timeBetweenReconnectMillis);
        poolConfiguration.setInterTransactionDelayMillis(config.timeBetweenTransactionsMillis);
        poolConfiguration.setReconnectAfterMillis(config.reconnectAfterMillis);
    }

    public void initialize() {

        managerRef.get().addListener(this);
        managerRef.get().setEndpointPoolConfiguration(endpoint, poolConfiguration);

        modbusPollers.initializePollers();
    }

    public void dispose() {
        modbusPollers.dispose();
    }

    @Override
    synchronized public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint otherEndpoint,
            @Nullable EndpointPoolConfiguration otherPoolConfiguration) {
        if (endpoint == null) {
            return;
        }
        EndpointPoolConfiguration poolConfiguration = this.poolConfiguration;

        if (poolConfiguration != null && otherEndpoint.equals(this.endpoint)
                && !poolConfiguration.equals(otherPoolConfiguration)) {
            this.handler.updateThingStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    this.formatConflictingParameterError(otherPoolConfiguration));
        }

    }

    private String formatConflictingParameterError(@Nullable EndpointPoolConfiguration otherPoolConfig) {
        Thing thing = this.handler.getThing();

        return String.format(
                "Endpoint '%s' has conflicting parameters: parameters of this thing (%s '%s') %s are different from some other things parameter: %s. Ensure that all endpoints pointing to tcp slave '%s:%s' have same parameters.",
                endpoint, thing.getUID(), thing.getLabel(), this.poolConfiguration, otherPoolConfig,
                Optional.ofNullable(this.endpoint).map(e -> e.getAddress()).orElse("<null>"),
                Optional.ofNullable(this.endpoint).map(e -> String.valueOf(e.getPort())).orElse("<null>"));
    }

    ModbusTCPSlaveEndpoint getSlaveEndpoint() {
        return this.endpoint;
    }

    Supplier<ModbusManager> getManagerRef() {
        return this.managerRef;
    }

    ModbusManager getManager() {
        if (this.managerRef != null) {
            return this.managerRef.get();
        }

        return null;
    }

    public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request,
            ModbusRegisterArray registers) {

        Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();

        KitaHeatPump kita = this.handler.getKita();
        kita.getData().forEach((type, value) -> {

            ModbusReadFunctionCode fnCode = ModbusConversionUtils.convertToModbusReadFunctionCode(type.register);
            if (!fnCode.equals(request.getFunctionCode())) {
                return;
            }

            int readIndex = type.address;
            int pollStart = request.getReference();
            int extractIndex = readIndex - pollStart;
            int dataLen = request.getDataLength();

            if (extractIndex > 0 && extractIndex < dataLen) {
                // data value is in the resonse buffer (registers) bounds

                ModbusConstants.ValueType readValueType = ModbusConversionUtils.convertToValueType(type.type);

                Optional<DecimalType> numericStateOpt = ModbusBitUtilities.extractStateFromRegisters(registers,
                        extractIndex, readValueType);

                boolean boolValue = (numericStateOpt.isPresent()) && (!numericStateOpt.get().equals(DecimalType.ZERO));

                String id = type.name;
                ChannelUID channelUID = this.handler.getChannelsHandler().getChannelUID(id);
                if (numericStateOpt.isPresent()) {
                    DecimalType numericState = numericStateOpt.get();
                    DecimalType numericStateConverted = ModbusConversionUtils.convertState(type, numericState);

                    ret.put(channelUID, numericStateConverted);
                } else {
                    ret.put(channelUID, UnDefType.UNDEF);

                }
            }

        });

        this.handler.getChannelsHandler().updateExpiredChannels(ret);
        return ret;
    }

    public Map<ChannelUID, State> processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray) {
        Map<ChannelUID, State> ret = new HashMap<ChannelUID, State>();

        KitaHeatPump kita = this.handler.getKita();
        kita.getData().forEach((type, value) -> {

            ModbusReadFunctionCode fnCode = ModbusConversionUtils.convertToModbusReadFunctionCode(type.register);
            if (!fnCode.equals(request.getFunctionCode())) {
                return;
            }

            int readIndex = type.address;
            int pollStart = request.getReference();
            int extractIndex = readIndex - pollStart;
            int dataLen = request.getDataLength();

            if (extractIndex > 0 && extractIndex < dataLen) {
                // data value is in the resonse buffer (bitsarray) bounds
                boolean boolValue = bitsarray.getBit(extractIndex);

                DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;

                String id = type.name;
                ChannelUID channelUID = this.handler.getChannelsHandler().getChannelUID(id);
                ret.put(channelUID, numericState);
            }

        });

        this.handler.getChannelsHandler().updateExpiredChannels(ret);

        return ret;
    }

    public void writeData(ChannelUID channelUID, @NonNull Command command) {

        ModbusWriter writer = new ModbusWriter(this.handler);
        writer.writeData(channelUID, command);

    }

}
