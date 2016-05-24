package com.elyxor.xeros.ldcs.thingworx;

import com.elyxor.xeros.ldcs.reliagate.GlobalControllerPortMap;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinitions;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.collections.FieldDefinitionCollection;
import com.thingworx.types.BaseTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
//Event Definitions
@ThingworxEventDefinitions(events = {
	@ThingworxEventDefinition(name="SystemEnabled", description="System Enabled", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BwDoseDone", description="Brightwell Dose Done", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="ColdWaterFillStep", description="Cold Water Fill", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="HotWaterFillStep", description="Hot Water Fill", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BwDosingStep", description="Brightwell Dosing Step", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="CupDosingStep", description="Cup Dosing Step", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankToSumpStep", description="Tank To Sump Step", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="MachineHeatStep", description="MachineHeatStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankHeatStep", description="TankHeatStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BeadsStep", description="BeadsStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DrainStep", description="DrainStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TumbleStep", description="TumbleStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="ExtractStep", description="ExtractStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankSprayStep", description="TankSprayStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpSprayStep", description="SumpSprayStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BwDosingEnabled", description="BwDosingEnabled", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="EndOfWashProgramStep", description="EndOfWashProgramStep", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DrumRotationFault", description="DrumRotationFault", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="ExternalDosingFault", description="ExternalDosingFault", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpLevelOverflow", description="SumpLevelOverflow", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BeadPulseTimeout", description="BeadPulseTimeout", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpLevelOutOfRange", description="SumpLevelOutOfRange", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankLevelOutOfRange", description="TankLevelOutOfRange", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankTempOutOfRange", description="TankTempOutOfRange", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpWaterFillEvent", description="SumpWaterFillEvent", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="ExtraWaterAddedEvent", description="ExtraWaterAddedEvent", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="OutOfBalanceWarningAlarm", description="OutOfBalanceWarningAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="OutOfBalanceAlarm", description="OutOfBalanceAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DrumDoorNotClosedAlarm", description="DrumDoorNotClosedAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankHeatingAlarm", description="TankHeatingAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BeadPumpOLAlarm", description="BeadPumpOLAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="WaterPumpOLAlarm", description="WaterPumpOLAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="PlcBatteryAlarm", description="PlcBatteryAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="InverterFailedToStartAlarm", description="InverterFailedToStartAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="TankNoFillAlarm", description="TankNoFillAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpNoFillAlarm", description="SumpNoFillAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BwFail", description="BwFail", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpTempOutOfRangeAlarm", description="SumpTempOutOfRangeAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpTopUpActiveEvent", description="SumpTopUpActiveEvent", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="SumpRefillActiveEvent", description="SumpRefillActiveEvent", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="EStopPushedAlarm", description="EStopPushedAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="InverterFaultAlarm", description="InverterFaultAlarm", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="CycleStart", description="CycleStart", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="CycleStop", description="CycleStop", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="CyclePause", description="CyclePause", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="BwStart", description="BwStart", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DoseCup", description="DoseCup", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DrumMotorOk", description="DrumMotorOk", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
	@ThingworxEventDefinition(name="DrumDoorLocked", description="DrumDoorLocked", dataShape="SensorEvent", category="Sensor", isInvocable=true, isPropertyEvent=false),
    @ThingworxEventDefinition(name="CycleEvent", description="Cycle has completed Event", dataShape="CycleComplete", category="Complete", isInvocable=true, isPropertyEvent=false)
})

@ThingworxPropertyDefinitions(properties = {
        @ThingworxPropertyDefinition(name="TankLevel", description="TankLevel", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="TankTemp", description="TankTemp", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="SumpLevel", description="SumpLevel", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="SumpTemp", description="SumpTemp", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="BwProgramNo", description="BwProgramNo", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="DrumGs", description="DrumGs", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="CurrentProgTime", description="CurrentProgTime", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="AdjustedProgTime", description="AdjustedProgTime", baseType="NUMBER", category="Status", aspects={"isReadOnly:true"})
})

public class XerosWasherGlobalThing extends VirtualThing implements Runnable {
    private static final String TAG = XerosWasherGlobalThing.class.getName();
	//SensorData
	private static final String START_TIME = "Time";
	private static final String DURATION ="Duration";
	private static final String SENSOR_NAME = "SensorName";


    //CycleComplete
    private static final String WM1_LIFETIME ="WM1_Lifetime";
    private static final String WM1_DAY = "WM1_Day";
    private static final String WM1_CYCLE = "WM1_Cycle";
    private static final String WM2_LIFETIME ="WM2_Lifetime";
    private static final String WM2_DAY = "WM2_Day";
    private static final String WM2_CYCLE = "WM2_Cycle";
    private static final String SENSOR_DATA ="Sensor_Data";
    //SensorData
    private static final String TIME = "Time";
    private static final String SENSORID = "SensorID";

    private ValueCollection eventInProgress;

    final static Logger logger = LoggerFactory.getLogger(XerosWasherGlobalThing.class);

    private GlobalControllerPortMap map;

    @Override
	public void run() {
		try {
			// Delay for a period to verify that the Shutdown service will return
			Thread.sleep(1000);
			// Shutdown the client
			this.getClient().shutdown();
		} catch (Exception x) {
			// Not much can be done if there is an exception here
			// In the case of production code should at least log the error
		}
	}

	public XerosWasherGlobalThing(String name, String description, String identifier, ConnectedThingClient client) {
		super(name,description,identifier,client);
		// Data Shape definition that is used for individual sensor events from the Global Controller
		
		FieldDefinitionCollection sensorEventFields = new FieldDefinitionCollection();

        sensorEventFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.SENSOR_NAME, BaseTypes.STRING));
        sensorEventFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.START_TIME, BaseTypes.DATETIME));
        sensorEventFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.DURATION, BaseTypes.NUMBER));
		defineDataShapeDefinition("SensorEvent",sensorEventFields);

        FieldDefinitionCollection sensorFields = new FieldDefinitionCollection();

        sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.SENSORID, BaseTypes.STRING));
        sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.TIME, BaseTypes.DATETIME));
        sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.DURATION, BaseTypes.NUMBER));
        defineDataShapeDefinition("SensorData",sensorFields);


        FieldDefinitionCollection cycleFields = new FieldDefinitionCollection();
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM1_LIFETIME,BaseTypes.NUMBER));
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM1_DAY,BaseTypes.NUMBER));
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM1_CYCLE,BaseTypes.NUMBER));
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM2_LIFETIME,BaseTypes.NUMBER));
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM2_DAY,BaseTypes.NUMBER));
        cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherGlobalThing.WM2_CYCLE,BaseTypes.NUMBER));
        FieldDefinition sensorDataField = new FieldDefinition(XerosWasherGlobalThing.SENSOR_DATA,BaseTypes.INFOTABLE);

        sensorDataField.setLocalDataShape(this.getDataShapeDefinition("SensorData"));
        cycleFields.addFieldDefinition(sensorDataField);

        defineDataShapeDefinition("CycleComplete", cycleFields);

        this.map = new GlobalControllerPortMap();
        // Populate the thing shape with the properties, services, and events that are annotated in this code
		super.initializeFromAnnotations();
	}

    public void sendEventToThingworx(int portNum, DateTime startTime) {
        ValueCollection event = new ValueCollection();
        event.put(SENSOR_NAME, new StringPrimitive(map.findPort(portNum).getPortName()));
        event.put(START_TIME, new DatetimePrimitive(startTime));
        event.put(DURATION, new NumberPrimitive(DateTime.now().minus(startTime.getMillis()).getMillis()));
        super.queueEvent(map.findPort(portNum).getPortName(), DateTime.now(), event);
        try {
            super.updateSubscribedEvents(60000);
        } catch (Exception e) {
            logger.warn(TAG, "failed to update subscribed events");
        }
    }

    public void sendProperty(int portNum, int value) {
        try {
            super.setProperty(map.findPort(portNum).getPortName(), value);
            super.updateSubscribedProperties(15000);
        } catch (Exception e) {
            logger.warn(TAG, "failed to update property " + map.getRegisterMap().get(portNum));
        }
    }

    // From the VirtualThing class
    // This method will get called when a connect or reconnect happens
    // Need to send the values when this happens
    // This is more important for a solution that does not send its properties on a regular basis
    public void synchronizeState() {
        // Be sure to call the base class
        super.synchronizeState();
        // Send the property values to Thingworx when a synchronization is required
        super.syncProperties();
    }
    public void createCycleCompleteEvent(double WM1_LifeTime,double WM1_Day,double WM1_Cycle,double WM2_LifeTime,double WM2_Day,double WM2_Cycle)
    {
        System.out.println(this.getName()+" started to create CycleComplete Event");
        this.eventInProgress = new ValueCollection();

        this.eventInProgress.put(XerosWasherGlobalThing.WM1_LIFETIME, new NumberPrimitive(WM1_LifeTime));
        this.eventInProgress.put(XerosWasherGlobalThing.WM1_DAY, new NumberPrimitive(WM1_Day));
        this.eventInProgress.put(XerosWasherGlobalThing.WM1_CYCLE, new NumberPrimitive(WM1_Cycle));
        this.eventInProgress.put(XerosWasherGlobalThing.WM2_LIFETIME, new NumberPrimitive(WM2_LifeTime));
        this.eventInProgress.put(XerosWasherGlobalThing.WM2_DAY, new NumberPrimitive(WM2_Day));
        this.eventInProgress.put(XerosWasherGlobalThing.WM2_CYCLE, new NumberPrimitive(WM2_Cycle));

        InfoTable data = new InfoTable();
        data.setDataShape(this.getDataShapeDefinition("SensorData").clone());
        this.eventInProgress.put(XerosWasherGlobalThing.SENSOR_DATA, new InfoTablePrimitive(data));
    }

    public void addSensorDataToCycleCompleteEvent(String sensorID, DateTime time, double duration)
    {
        System.out.println(this.getName()+" added sensor data to CycleComplete Event");

        InfoTablePrimitive primitiveInfoTable  = (InfoTablePrimitive) this.eventInProgress.getPrimitive(XerosWasherGlobalThing.SENSOR_DATA);
        InfoTable data = primitiveInfoTable.getValue();

        ValueCollection dataInfo = new ValueCollection();
        dataInfo.put(XerosWasherGlobalThing.SENSORID, new StringPrimitive(sensorID));
        dataInfo.put(XerosWasherGlobalThing.TIME, new DatetimePrimitive(time));
        dataInfo.put(XerosWasherGlobalThing.DURATION, new NumberPrimitive(duration));

        data.addRow(dataInfo);
        this.eventInProgress.put(XerosWasherGlobalThing.SENSOR_DATA, new InfoTablePrimitive(data));
    }

    public void sendCycleCompleteEvent() throws Exception {
        System.out.println(this.getName()+" send CycleComplete Event to server");
        super.queueEvent("CycleEvent", DateTime.now(), this.eventInProgress);
        super.updateSubscribedEvents(60000);
    }

    public void parseLogToThingWorx(File file) {
        DaiCollectionParser parser = new DaiCollectionParser();
        Map<String, String> fileMeta = new HashMap<String, String>();
        List<DaiMeterCollection> collections = null;
        DateTime time = new DateTime();
        String timezone = time.getZone().toString();
        fileMeta.put("olson_timezone_id", timezone);
        fileMeta.put("current_system_time", String.valueOf(System.currentTimeMillis()));
        String fileCreated = "";
        BasicFileAttributes attributes = null;

        try {
            attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            fileCreated = String.valueOf(attributes.creationTime().toMillis());
        } catch (IOException e) {
            logger.warn("Unable to get File Create Time: ", e.getMessage());
        }
        fileMeta.put("file_create_time", fileCreated);
        try {
            collections = parser.parse(file, fileMeta);
        } catch (Exception e) {
            logger.warn("Failed to parse file: ", e.getMessage());
        }
        sendToThingWorx(collections);
    }

    private void sendToThingWorx(List<DaiMeterCollection> collections) {
        for (DaiMeterCollection collection : collections) {
            double coldWater = 0;
            double hotWater = 0;
            for (DaiMeterCollectionDetail detail : collection.getCollectionDetails()) {
                if (detail.getMeterType().equals("WM0") || detail.getMeterType().equals("WM2")) {
                    coldWater = detail.getDuration();
                    logger.debug("Thingworx cold water: " + coldWater);
                } else if (detail.getMeterType().equals("WM1") || detail.getMeterType().equals("WM3")) {
                    hotWater = detail.getDuration();
                    logger.debug("Thingworx hot water: " + coldWater);
                }
            }
            this.createCycleCompleteEvent(0, 0, coldWater, 0, 0, hotWater);

            for (DaiMeterCollectionDetail detail : collection.getCollectionDetails()) {
                if (!detail.getMeterType().startsWith("WM")) {
                    String sensorId = detail.getMeterType();
                    DateTime dateTime = new DateTime(detail.getTimestamp());
                    double dur = detail.getDuration();
                    this.addSensorDataToCycleCompleteEvent(sensorId, dateTime, dur);
                }
            }

            try {
                this.sendCycleCompleteEvent();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }


}
