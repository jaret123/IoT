package com.elyxor.xeros.ldcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.dai.DaiPortInterface;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialReader implements SerialPortEventListener, SerialReaderInterface {
	
	final static Logger logger = LoggerFactory.getLogger(SerialReader.class);
	private static final String EOT = new String(new char[] {'','',''}); 
	
	SerialPort serialPort;
	DaiPortInterface daiPort;
	
	public SerialReader(DaiPortInterface port)  {
    	daiPort = port;
		serialPort = port.getSerialPort();
	} 
	
	public void serialEvent(SerialPortEvent event) {
		String eventBuffer = "";
		String logBuffer;
		
		if (event.getEventValue() > 2 && event.getEventValue() < 5) {
			try {
				eventBuffer = serialPort.readString();
			} catch (SerialPortException e) {
				logger.warn("Unable to read port event", e);
			}
			if (!eventBuffer.isEmpty() && eventBuffer.equals("***")) {
				logger.info("Log file incoming");
				logBuffer = daiPort.sendRequest();
				if (!logBuffer.isEmpty() && logBuffer.endsWith(EOT)) {
					logger.info("Proper log file found, writing...");
					daiPort.writeLogFile(logBuffer);
				}
			}
		}
	}	
}
