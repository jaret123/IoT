package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.thingworx.ThingWorxClient;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.common.SecurityClaims;
import jssc.SerialPort;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PortManager implements PortManagerInterface, PortChangedListenerInterface {

	final static Logger logger = LoggerFactory.getLogger(PortManager.class);
	private String daiPrefix = AppConfiguration.getDaiName();
	Integer waterOnly = AppConfiguration.getWaterOnly();
	
	private String currentDir = Paths.get("").toAbsolutePath().getParent().toString();
	private Path path = Paths.get(currentDir, "/input");
	
	private static Map<String,DaiPortInterface> portList = new LinkedHashMap<String,DaiPortInterface>();

	PortFinderInterface _pf;
    JobSchedulerInterface jobScheduler;
	private int nextDaiNum = 1;

    private ThingWorxClient _client = null;

	public void getPortFinder(PortFinderInterface pfi) {
		if (null != pfi) {
			_pf = pfi;
			_pf.addListener(this);
			logger.info("Started watching ports for changes");
		}
	}

    public void setThingWorxClient(ThingWorxClient thingWorxClient) {
        this._client = thingWorxClient;
    }
    public JobSchedulerInterface getJobScheduler() {
        if (jobScheduler == null) {
            jobScheduler = new JobScheduler(this, waterOnly);
        }
        return jobScheduler;
    }
		
	public boolean portAdded(String portName) {
        logger.info("Found new USB device: "+portName);
		if (waterOnly == 2) {
			WaterMeterPortInterface waterMeterPort = new WaterMeterPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"), daiPrefix, null);
			if (waterMeterPort.openPort()) {
				String id =  waterMeterPort.initRequest();
                if (id == null) {
                    waterMeterPort.closePort();
                    return false;
                }
                waterMeterPort.setLogWriter(new FileLogWriter(path, daiPrefix + waterMeterPort.getWaterMeterId() + "Log.txt"));
				portList.put(portName, waterMeterPort);
				nextDaiNum++;
                logger.info("Added EK meter with ID: " + daiPrefix + waterMeterPort.getWaterMeterId());
				return true;
			}
		}
		DaiPortInterface daiPort = new DaiPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"), daiPrefix, null);
		String daiId = null;
		String newId = null;
		int retryCounter = 0;
        int daiIdInt = 0;

		if (daiPort.openPort()) {
            while (retryCounter < 3) {
			    daiId = daiPort.getRemoteDaiId();
			    if (daiId != null) {
                    try {
                        daiIdInt = Integer.parseInt(daiId.trim());
                    } catch (NumberFormatException e) {
                        logger.warn("failed to parse integer", e.getMessage());
                    }
                    logger.info("DAI ID is: '" +daiIdInt+"'");
                    if (daiIdInt > 0) {
                        daiPort.setDaiNum(daiIdInt);
                        logger.info("Found existing DAI with ID "+daiPrefix+daiIdInt+" on port"+portName);
                        nextDaiNum = daiIdInt + 1;
                        break;
                    }
                }
                retryCounter++;
			}
            if (daiId == null || daiId.equals("-1")) {
                daiPort.closePort();
                return false;
            }

            if (waterOnly == 1||waterOnly==3) {
                logger.info("init request");
                daiPort.initWaterRequest();
            }
            if (waterOnly == 3) {
                logger.info("init xeros request");
                daiPort.initXerosWaterRequest();
            }

            String daiIdentifier = daiPrefix+daiPort.getDaiNum();
            daiPort.setLogWriter(new FileLogWriter(path, daiIdentifier+"Log.txt"));

//            XerosWasherThing thing = new XerosWasherThing(daiIdentifier, daiIdentifier, daiIdentifier, _client);

//            try {
//                _client.bindThing(thing);
//            } catch (Exception e) {
//                logger.warn("can't bind thing: ", e.getMessage());
//            }
//            daiPort.setXerosWasherThing(thing);

			portList.put(portName, daiPort);
			return true;
		}
		return false;
	}
	
	public boolean portRemoved(String portName) {
        logger.info("USB Device removed: "+portName);
		DaiPortInterface daiPort = findDaiPort(portName);
		if (daiPort != null) {
			if (daiPort.closePort()) {
				portList.remove(daiPort.getSerialPort().getPortName());
				nextDaiNum = daiPort.getDaiNum();
                return true;
			}
			// port close failed
			return false;
		}
		// port already removed
		return false;
	}

    public Map<String, DaiPortInterface> getPortList() {
        return portList;
    }
	
	public List<String> getPortNameList()  {
		List<String> result = new ArrayList<String>();
		if (!portList.isEmpty()) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
                if (waterOnly == 2)
                    result.add(entry.getKey() + ", " + daiPrefix + ((WaterMeterPortInterface)entry.getValue()).getWaterMeterId());
                else
				    result.add(entry.getKey() + ", " + daiPrefix + entry.getValue().getDaiNum());
			}
		}
		return result;
	}
	
	public DaiPortInterface findDaiPort(String portName) {
		DaiPortInterface daiPort = null;
		if (!portName.isEmpty()) {
			daiPort = portList.get(portName);
		}
		return daiPort;
	}
	
	public DaiPortInterface findDaiPort(int id) {
		DaiPortInterface daiPort = null;
		if (id > 0) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
				if (entry.getValue().getDaiNum() == id) {
					daiPort = entry.getValue();
				}
			}
		}
		return daiPort;
	}
	
    public void shutdownListener() {
		_pf.setRunning(false);
		logger.info("Stopped listening for new ports");
		for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
			DaiPortInterface port = entry.getValue();
			port.closePort();
		}
	}
}
