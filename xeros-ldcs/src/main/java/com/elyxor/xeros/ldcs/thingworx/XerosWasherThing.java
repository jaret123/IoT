package com.elyxor.xeros.ldcs.thingworx;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinitions;
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
	@ThingworxEventDefinition(name="CycleEvent", description="Cycle has completed Event", dataShape="CycleComplete", category="Complete", isInvocable=true, isPropertyEvent=false)
})


public class XerosWasherThing extends VirtualThing implements Runnable {

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
	private static final String DURATION ="Duration";
	private static final String SENSORID = "SensorID";
	
	private ValueCollection eventInProgress;

    final static Logger logger = LoggerFactory.getLogger(XerosWasherThing.class);

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
	
	public XerosWasherThing (String name, String description, String identifier, ConnectedThingClient client) {
		super(name,description,identifier,client);
		
		// Data Shape definition that is used for end of cycle data
		
		FieldDefinitionCollection sensorFields = new FieldDefinitionCollection();
		
		sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.SENSORID, BaseTypes.STRING));
		sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.TIME, BaseTypes.DATETIME));
		sensorFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.DURATION, BaseTypes.NUMBER));
		defineDataShapeDefinition("SensorData",sensorFields);

		
		FieldDefinitionCollection cycleFields = new FieldDefinitionCollection();
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM1_LIFETIME,BaseTypes.NUMBER));
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM1_DAY,BaseTypes.NUMBER));
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM1_CYCLE,BaseTypes.NUMBER));
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM2_LIFETIME,BaseTypes.NUMBER));
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM2_DAY,BaseTypes.NUMBER));
		cycleFields.addFieldDefinition(new FieldDefinition(XerosWasherThing.WM2_CYCLE,BaseTypes.NUMBER));
		FieldDefinition sensorDataField = new FieldDefinition(XerosWasherThing.SENSOR_DATA,BaseTypes.INFOTABLE);

		sensorDataField.setLocalDataShape(this.getDataShapeDefinition("SensorData"));
		cycleFields.addFieldDefinition(sensorDataField);
	
		defineDataShapeDefinition("CycleComplete", cycleFields);
	
		// Populate the thing shape with the properties, services, and events that are annotated in this code
		super.initializeFromAnnotations();
	}
	// The processScanRequest is called by the SteamSensorClient every scan cycle
	@Override
	public void processScanRequest() throws Exception {
		// Be sure to call the base classes scan request
		super.processScanRequest();
		// Execute the code for this simulation every scan
		this.scanDevice();
	}
	
	// Performs the logic for the steam sensor, occurs every scan cycle
	public void scanDevice() throws Exception {
		
		// Update the subscribed properties and events to send any updates to Thingworx
		// Without calling these methods, the property and event updates will not be sent
		// The numbers are timeouts in milliseconds.
		super.updateSubscribedProperties(15000);
		super.updateSubscribedEvents(60000);
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
	  
	  this.eventInProgress.put(XerosWasherThing.WM1_LIFETIME, new NumberPrimitive(WM1_LifeTime));
	  this.eventInProgress.put(XerosWasherThing.WM1_DAY, new NumberPrimitive(WM1_Day));
	  this.eventInProgress.put(XerosWasherThing.WM1_CYCLE, new NumberPrimitive(WM1_Cycle));
	  this.eventInProgress.put(XerosWasherThing.WM2_LIFETIME, new NumberPrimitive(WM2_LifeTime));
	  this.eventInProgress.put(XerosWasherThing.WM2_DAY, new NumberPrimitive(WM2_Day));
	  this.eventInProgress.put(XerosWasherThing.WM2_CYCLE, new NumberPrimitive(WM2_Cycle));
	  
	  InfoTable data = new InfoTable();
	  data.setDataShape(this.getDataShapeDefinition("SensorData").clone());
	  this.eventInProgress.put(XerosWasherThing.SENSOR_DATA, new InfoTablePrimitive(data));	
  }
  
  public void addSensorDataToCycleCompleteEvent(String sensorID, DateTime time, double duration)
  {
	 System.out.println(this.getName()+" added sensor data to CycleComplete Event");
	 
	 InfoTablePrimitive primitiveInfoTable  = (InfoTablePrimitive) this.eventInProgress.getPrimitive(XerosWasherThing.SENSOR_DATA);
	 InfoTable data = primitiveInfoTable.getValue();
	 
	 ValueCollection dataInfo = new ValueCollection();
	 dataInfo.put(XerosWasherThing.SENSORID, new StringPrimitive(sensorID));
	 dataInfo.put(XerosWasherThing.TIME, new DatetimePrimitive(time));
	 dataInfo.put(XerosWasherThing.DURATION, new NumberPrimitive(duration));
	
	 data.addRow(dataInfo);
	 this.eventInProgress.put(XerosWasherThing.SENSOR_DATA, new InfoTablePrimitive(data));
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
                } else if (detail.getMeterType().equals("WM1") || detail.getMeterType().equals("WM3")) {
                    hotWater = detail.getDuration();
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
