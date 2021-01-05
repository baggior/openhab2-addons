package org.openhab.binding.mykitaheatpump.internal.modbus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mykitaheatpump.internal.MyKitaHeatPumpBindingConstants;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.DataTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaHeatPumpDataItem.RegisterTypeEnum;
import org.openhab.binding.mykitaheatpump.internal.kita.KitaManager;
import org.openhab.core.io.transport.modbus.BitArray;
import org.openhab.core.io.transport.modbus.ModbusBitUtilities;
import org.openhab.core.io.transport.modbus.ModbusConstants;
import org.openhab.core.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.core.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.exception.ModbusConnectionException;
import org.openhab.core.io.transport.modbus.exception.ModbusTransportException;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class ModbusPollers extends ModbusManagerBase implements ModbusResponseHandler {

    private static final int MAX_POLLER_LENGTH = 120;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // final List<DataValuePoller> pollers = new ArrayList<DataValuePoller>();
    final List<ModbusDataValuePoller> pollers = new ArrayList<ModbusDataValuePoller>();

    public ModbusPollers(KitaManager kitaManager) {
        super(kitaManager);
    }

    public void initializePollers() {
        logger.debug("ModbusMasterService initialize pollers() ");

        final Map<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller> _tempPollers = new HashMap<ModbusReadFunctionCode, @Nullable ModbusDataValuePoller>();

        kitaManager.getKita().getDataStore().forEach((dataType, dataValue) -> {
            // Todo

            RegisterTypeEnum register = dataType.register;
            int address = dataType.address;

            ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(register);
            ModbusDataValuePoller poller = _tempPollers.get(fnCode);
            if (poller == null) {
                int startAddress = address;
                int length = 1;
                poller = new ModbusDataValuePoller(fnCode, startAddress, length,
                        kitaManager.getConfiguration().maxTries, kitaManager.getConfiguration().refresh);

            } else {
                int startAddress = address;
                int oldEndAddress = poller.start + poller.length;
                int newEndAddress = startAddress + 1;
                if (startAddress < poller.start) {
                    poller.start = startAddress;
                    poller.length = oldEndAddress - startAddress;
                } else if (newEndAddress > oldEndAddress) {
                    poller.length = newEndAddress - poller.start;
                }

            }
            _tempPollers.put(fnCode, poller);
        });

        // build pollers map
        _tempPollers.values().forEach((poller) -> {
            if (poller != null) {
                if (poller.length > MAX_POLLER_LENGTH) {
                    this.logger.info(
                            "Split poller {} \npoller.length troppo grande: {}>" + MAX_POLLER_LENGTH
                                    + " => Superata la dimenzione massima di 256byte per un pacchetto MODBUS",
                            poller, poller.length);
                    // throw new RuntimeException("poller.length troppo grande " + poller.length
                    // + " > 120! => Superata la dimenzione massima di 256byte per un pacchetto MODBUS");

                    // split poller
                    List<ModbusDataValuePoller> splittedPollers = this.splitPoller(poller, MAX_POLLER_LENGTH);
                    this.logger.debug("Split poller {} to\n {}>", poller, splittedPollers);

                    pollers.addAll(splittedPollers);

                } else {
                    pollers.add(poller);
                }
            }
        });

        // start pollTask
        pollers.forEach((poller) -> {
            poller.registerPollTask(this, kitaManager.getKitaHeatPumpThingHandler(),
                    kitaManager.getCommunicationInterface());
        });
    }

    public void dispose() {

        pollers.forEach(poller -> {
            poller.unregisterPollTask(kitaManager.getKitaHeatPumpThingHandler(),
                    kitaManager.getCommunicationInterface());
        });
        pollers.clear();

    }

    @Override
    public synchronized void processUpdateStates(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        if (!this.kitaManager.isActive()) {
            logger.warn("kitaManager not active");
            return;
        }

        Map<ChannelUID, State> newChannelsState = new HashMap<ChannelUID, State>();

        this.kitaManager.getKita().getDataStore().forEach((type, value) -> {

            String id = type.name;
            ChannelUID channelUID = this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler()
                    .getChannelUID(id);
            if (channelUID != null) {

                ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
                if (!fnCode.equals(request.getFunctionCode())) {
                    return;
                }

                int readIndex = type.address;
                int pollStart = request.getReference();
                int extractIndex = readIndex - pollStart;
                int dataLen = request.getDataLength();

                if (extractIndex > 0 && extractIndex < dataLen) {

                    ModbusConstants.ValueType readValueType = this.convertToValueType(type.type);

                    Optional<DecimalType> numericStateOpt = ModbusBitUtilities.extractStateFromRegisters(registers,
                            extractIndex, readValueType);

                    boolean boolValue = (numericStateOpt.isPresent())
                            && (!numericStateOpt.get().equals(DecimalType.ZERO));

                    if (numericStateOpt.isPresent()) {
                        DecimalType numericState = numericStateOpt.get();
                        DecimalType numericStateConverted = this.convertState(type, numericState);

                        newChannelsState.put(channelUID, numericStateConverted);
                    } else {
                        newChannelsState.put(channelUID, UnDefType.UNDEF);
                    }
                }
            }

        });

        this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler().updateChannelsState(newChannelsState);
    }

    @Override
    public synchronized void processUpdateStates(ModbusReadRequestBlueprint request, BitArray bitsarray) {
        if (!this.kitaManager.isActive()) {
            logger.warn("kitaManager not active");
            return;
        }

        Map<ChannelUID, State> newChannelsState = new HashMap<ChannelUID, State>();

        this.kitaManager.getKita().getDataStore().forEach((type, value) -> {

            String label = type.name;
            ChannelUID channelUID = this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler()
                    .getChannelUID(label);
            if (channelUID != null) {

                ModbusReadFunctionCode fnCode = this.convertToModbusReadFunctionCode(type.register);
                if (!fnCode.equals(request.getFunctionCode())) {
                    return;
                }

                int readIndex = type.address;
                int pollStart = request.getReference();
                int extractIndex = readIndex - pollStart;

                boolean boolValue = bitsarray.getBit(extractIndex);

                DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;

                newChannelsState.put(channelUID, numericState);
            }

        });

        this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler().updateChannelsState(newChannelsState);

    }

    @Override
    public synchronized void processOnReadError(ModbusReadRequestBlueprint request, Exception error) {
        if (!this.kitaManager.isActive()) {
            logger.warn("kitaManager not active");
            return;
        }
        if (error instanceof ModbusConnectionException) {
            logger.trace("Thing {} '{}' had {} error on read: {}",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else if (error instanceof ModbusTransportException) {
            logger.trace("Thing {} '{}' had {} error on read: {}",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else {
            logger.error(
                    "Thing {} '{}' had {} error on read: {} (message: {}). Stack trace follows since this is unexpected error.",
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getUID(),
                    this.kitaManager.getKitaHeatPumpThingHandler().getThing().getLabel(), error.getClass().getName(),
                    error.toString(), error.getMessage(), error);
        }

        ChannelUID lastReadErrorUID = this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler()
                .getChannelUID(MyKitaHeatPumpBindingConstants.CHANNEL_LAST_READ_ERROR);
        if (lastReadErrorUID != null) {
            this.kitaManager.getKitaHeatPumpThingHandler().getChannelsHandler().updateChannelState(lastReadErrorUID,
                    new DateTimeType());
        }

    }

    private List<ModbusDataValuePoller> splitPoller(final ModbusDataValuePoller poller, final int maxLength) {

        // split poller ricorsivo

        List<ModbusDataValuePoller> ret = new ArrayList<ModbusDataValuePoller>();

        if (poller.length > maxLength) {
            ModbusDataValuePoller firstPoller = new ModbusDataValuePoller(poller.functionCode, poller.start,
                    MAX_POLLER_LENGTH, kitaManager.getConfiguration().maxTries, kitaManager.getConfiguration().refresh);

            ModbusDataValuePoller nextPoller = new ModbusDataValuePoller(poller.functionCode,
                    poller.start + MAX_POLLER_LENGTH, poller.length - MAX_POLLER_LENGTH,
                    kitaManager.getConfiguration().maxTries, kitaManager.getConfiguration().refresh);

            ret.add(firstPoller);
            ret.addAll(this.splitPoller(nextPoller, maxLength));
        } else {
            ret.add(poller);
        }

        return ret;

    }

    private ModbusReadFunctionCode convertToModbusReadFunctionCode(RegisterTypeEnum register) {
        switch (register) {
            case coil:
                return ModbusReadFunctionCode.READ_COILS;
            case discrete_input:
                return ModbusReadFunctionCode.READ_INPUT_DISCRETES;
            case holding:
                return ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS;
            case input:
                return ModbusReadFunctionCode.READ_INPUT_REGISTERS;

            default:
                throw new RuntimeException("RegisterTypeEnum unknown!");
        }
    }

    private DecimalType convertState(KitaHeatPumpDataItem dataType, DecimalType originalState) {
        double factor = this.readFactor(dataType);

        DecimalType convertedState = new DecimalType(originalState.doubleValue() * factor);
        return convertedState;
    }

    private double readFactor(KitaHeatPumpDataItem dataType) {
        if (dataType.register == RegisterTypeEnum.holding) {
            switch (dataType.type) {
                case _bool:
                case _switch:
                    return 1;

                case dateTime:
                    break;
                case number:
                    break;
                case string:
                    break;

                case cop:
                case flow_ro:
                case pct:
                case temperature_ro:
                case temperature_rw:
                case rps_ro:
                    return 0.1;
            }
        }
        return 1;
    }

    private ValueType convertToValueType(DataTypeEnum type) {
        switch (type) {
            case _bool:
                return ValueType.INT16;
            case _switch:
                return ValueType.INT16;
            case dateTime:
                return ValueType.INT16; // TODO

            case number:
            case pct:
            case cop:
            case flow_ro:
            case temperature_ro:
            case temperature_rw:
            case rps_ro:
                return ValueType.INT16;

            case string:
                throw new RuntimeException("invalid DataTypeEnum string!");
            default:
                throw new RuntimeException("DataTypeEnum unknown! -> " + type.toString());

        }

    }

}
