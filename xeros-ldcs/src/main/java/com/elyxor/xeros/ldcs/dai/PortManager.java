package com.elyxor.xeros.ldcs.dai;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.util.FileLogWriter;

import jssc.SerialPort;

public class PortManager implements PortManagerInterface, PortChangedListenerInterface {

	final static Logger logger = LoggerFactory.getLogger(PortManager.class);
	private String daiPrefix = AppConfiguration.getDaiName();
	private Path path = Paths.get(AppConfiguration.getLocalPath());
	
	private Map<String,DaiPortInterface> portList = new LinkedHashMap<String,DaiPortInterface>();

	PortFinderInterface _pf;
	private int nextDaiNum = 1;
	
	
	public void getPortFinder(PortFinderInterface pfi) {
		if (null != pfi) {
			_pf = pfi;
			_pf.addListener(this);
		}
	}
	
	public boolean portAdded(String portName) {
		DaiPortInterface daiPort = new DaiPort(new SerialPort(portName), nextDaiNum, new FileLogWriter(path, daiPrefix+nextDaiNum+"Log.txt"));
		if (daiPort.openPort()) {
			portList.put(portName, daiPort);
			nextDaiNum++;
			return true;
		}
		return false;
	}
	
	public boolean portRemoved(String portName) {
		DaiPortInterface daiPort = findDaiPort(portName);
		if (daiPort != null) {
			if (daiPort.closePort()) {
				portList.remove(daiPort);
				nextDaiNum = daiPort.getDaiNum();
			}
			// port close failed ?
			return false;
		}
		// port already removed
		return false;
		
	}
	
	public List<String> getPortList()  {
		List<String> portListOut = new ArrayList<String>();
		if (!portList.isEmpty()) {
			for (Entry<String,DaiPortInterface> entry : portList.entrySet()) {
				portListOut.add(entry.getKey() + ", " + daiPrefix + entry.getValue().getDaiNum());
			}
		}
		return portListOut;
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
