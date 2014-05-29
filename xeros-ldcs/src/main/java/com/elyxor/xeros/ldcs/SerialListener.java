package com.elyxor.xeros.ldcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.dai.DaiPort;
import com.elyxor.xeros.ldcs.dai.DaiPortInterface;
import com.elyxor.xeros.ldcs.dai.PortManager;
import com.elyxor.xeros.ldcs.dai.PortManagerInterface;

public class SerialListener {
	
	final static Logger logger = LoggerFactory.getLogger(SerialListener.class);
	    
	static volatile boolean portFinderRunning;

	public static void main(String[] args) {
		(new Thread(new CommandListener())).start();
		(new Thread(new PortFinder())).start();
	}
		
	static class PortFinder implements Runnable {
		
		public void run() {
			logger.info("Watching for new serial devices");
			
			nextDaiNum = 1;
			portFinderRunning = true;
			
			while (portFinderRunning) {
				daiName = daiPrefix + nextDaiNum;

//				List<String> localPorts = Arrays.asList("/dev/pts/29", "/dev/pts/30");
				
				List<String> localPorts = Arrays.asList(SerialPortList.getPortNames());				
				List<String> activePortAddresses = new ArrayList<String>(portList.keySet());
				
				if (!portList.isEmpty()) {						
					for (String portAddress : activePortAddresses) {	
						if (!localPorts.contains(portAddress)) {
							DaiPort activePort = portList.get(portAddress);
							activePort.closeDaiPort();
							nextDaiNum = activePort.getDaiNum();
						}
					}
				}
				for (String portAddress : localPorts) {
					if (!portList.containsKey(portAddress)) {				
						DaiPort port = new DaiPort(new SerialPort(portAddress), nextDaiNum);
						port.openDaiPort();
						nextDaiNum++;
						break;
					}
				} 
			}
			if (Thread.interrupted()) {
				logger.info("Ending watch for new serial devices ");
				return;
			}
		}
	}
	            
    
/*    
    static class SerialReaderTest implements SerialPortEventListener {
    	SerialPort serialPort;
    	DaiPort daiPort;
    	
    	public SerialReaderTest(DaiPort port) {
    		daiPort = port;
    		serialPort = port.getSerialPort();
    	} 
    	
		public void serialEvent(SerialPortEvent event) {
			File file = null;
			Path dir;
			String buffer;
			int testNum = 1;
						
			if (event.isRXCHAR()) {
				if (event.getEventValue() == 2) {
					try {
						buffer = serialPort.readString();
						
						if (buffer.equals("11")) {
							serialPort.writeString(990+testNum+"\n");
						}
						else if (buffer.equals("12")) {
							daiPort.sendRequest();
						}
					} catch (Exception ex) {
						logger.warn("Failed to start data stream", ex);
					}
				}
				if (event.getEventValue() > 2) {
					try {
						buffer = serialPort.readString(event.getEventValue());

						if (buffer.endsWith("^M^M^M")) {
							dir = Paths.get(AppConfiguration.getLocalPath());
							file = new File(dir.toString(), "output.txt");
							FileWriter writer = new FileWriter(file, true);
							writer.write(daiName + buffer);
							writer.close();
							
							testNum++;
						}
					} catch (SerialPortException ex) {
						logger.warn("Failed to read data stream from "+serialPort.getPortName(), ex);
					} catch (IOException ex) {
						logger.warn("Failed to write log file to "+file.getName(), ex);
					}
				}				
			}
		}
    }
    */
}


