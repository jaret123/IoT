package com.elyxor.xeros.ldcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.dai.DaiPort;
import com.elyxor.xeros.ldcs.dai.DaiPortInterface;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

public class SerialReader implements SerialPortEventListener, SerialReaderInterface {
	
	final static Logger logger = LoggerFactory.getLogger(SerialReader.class);
	private static final String EOT = new String(new char[] {'','',''}); 
	
	SerialPort serialPort;
	DaiPortInterface daiPort;
	
	public SerialReader(DaiPortInterface port) {
    	daiPort = port;
		serialPort = port.getSerialPort();
	} 

	public void serialEvent(SerialPortEvent event) {
		String eventBuffer = "";
		String logBuffer;
		
//		if (event.isRXCHAR()) {
			if (event.getEventValue() > 2) {
				try {
					eventBuffer = serialPort.readString();
					if (!eventBuffer.isEmpty() && eventBuffer.equals("***\r\n")) {
						logBuffer = daiPort.sendRequest();
						if (!logBuffer.isEmpty() && logBuffer.endsWith("\r\n\r\n\r\n")) {
							daiPort.writeLogFile(logBuffer);
						}
					}
				} catch (Exception ex) {
					logger.warn("Failed to read serial event", ex);
				}

			}
//    	}
	}	
}
