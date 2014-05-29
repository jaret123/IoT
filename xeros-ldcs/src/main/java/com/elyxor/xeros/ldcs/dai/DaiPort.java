package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortEventListener;

import com.elyxor.xeros.ldcs.AppConfiguration;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;

public class DaiPort implements DaiPortInterface, SerialPortEventListener {

	final static Logger logger = LoggerFactory.getLogger(DaiPort.class);
	private static final String EOT = new String(new char[] {'\004','\012'}); 

	private static String daiName = "";
	
	private SerialPort serialPort;
	private int daiNum;

	private LogWriterInterface _logWriter;
	
	public DaiPort (SerialPort port, int num, LogWriterInterface logWriter) { 
		this.serialPort = port;
		this.daiNum = num;
		this._logWriter = logWriter;
	}
		

	public SerialPort getSerialPort() {
		return serialPort;
	}

	public void setSerialPort(SerialPort port) {
		this.serialPort = port;
	}

	public int getDaiNum() {
		return daiNum;
	}

	public void setDaiNum(int id) {
		this.daiNum = id;
	}
		
	public void serialEvent(SerialPortEvent event) {
		String eventBuffer = "";
		String logBuffer ;
		
		if (event.isRXCHAR()) {
			if (event.getEventValue() == 3) {
				try {
					eventBuffer = serialPort.readString(3);
				} catch (Exception ex) {
					logger.warn("Failed to read serial event", ex);
				}
				if (!eventBuffer.isEmpty() && eventBuffer.equals("***")) {
					logBuffer = this.sendRequest();
					if (!logBuffer.isEmpty() && logBuffer.endsWith(EOT)) {
						this.writeLogFile(logBuffer);
					}
				}
			}
    	}
	}	
	
	public boolean openPort() {
		SerialPort port = this.serialPort;
		String portAddress = port.getPortName();
		
		boolean result = false;
		String daiId = "";
		
		try {
			result = port.openPort();
			
			// Where do these magic values come from?  Should they be configurable?
			port.setParams(4800, 7, 1, 2, false, false);
			
			daiId = this.getRemoteDaiId();
										
			if (daiId == "0" || !(daiId == ""+nextDaiNum)) {
				this.setRemoteDaiId(nextDaiNum);
//				portList.put(portAddress, this);
		    	logger.info("Assigned DAI ID "+daiName+nextDaiNum+" to port "+portAddress);	
			}
			
			port.addEventListener(this); 
			
	    	logger.info("Started listening on port " + port.getPortName());

		} catch (Exception ex) {
			logger.warn("Could not open port", ex);
			result = false;
		}
		return result;
	}
	
	public String clearPortBuffer() {
		String result = "";
		try {
			result = this.serialPort.readString();
		}
		catch (Exception e) {
			String msg = "Failed to clear port buffer: ";
			logger.warn(msg , e);get
			result = msg + e.getMessage();
		}
		return result;
	}
	
	public String getRemoteDaiId() {
		String daiId = "";
		try {
			this.serialPort.writeString("19\n");
			daiId = this.serialPort.readString();
		}
		catch (Exception e) {
			logger.warn("Couldn't read dai id", e);
		}
		return daiId;
	}
	
	public String setRemoteDaiId(int id) {
		String daiId = "";
		try {
			this.serialPort.writeString("0 19 \n");
			Thread.sleep(2000);
			daiId = this.serialPort.readString();
			this.serialPort.writeString(id+"\n");
			Thread.sleep(5000);
			daiId += this.serialPort.readString();
		}
		catch (Exception e) {
			logger.warn("Couldn't set port id", e);
		}
		this.setDaiNum(id);
		return daiId;
	}
	
	public String sendStdRequest() {
		String buffer = "";	    		
		try {
			this.serialPort.writeString("0 12\n");
			Thread.sleep(10000);
			buffer = this.serialPort.readString();
		} catch (Exception e) {
			String msg = "Couldn't complete send std request. ";
			logger.warn(msg, e);
			buffer = msg + e.getMessage(); 
		}
		this.writeLogFile(buffer);
		return buffer;
	}
	
	public String sendXerosRequest() {
		String buffer = "";	    		
		try {
			this.serialPort.writeString("0 11\n");
			Thread.sleep(10000);
			buffer = this.serialPort.readString();
		} catch (Exception e) {
			logger.warn("Couldn't complete send xeros request", e);
		}
		return buffer;
	}
	
	public String sendRequest() {
		String buffer = "";	    		
		try {
			this.serialPort.writeString("0 999\n");
			Thread.sleep(10000);
			buffer = this.serialPort.readString();
		} catch (Exception e) {
			logger.warn("Couldn't complete send request", e);
		}
		return buffer;
	}
	
	public String setClock() {
		String buffer = "";
		try {
			this.serialPort.writeString("55\n");
			Thread.sleep(5000);
			
			SimpleDateFormat timingFormat = new SimpleDateFormat("hh:mm:ss");
			buffer = timingFormat.format(System.currentTimeMillis());
			String[] timeSplit = buffer.split(":");
			
			this.serialPort.writeString(timeSplit[0]+"\n");
			this.serialPort.writeString(timeSplit[1]+"\n");
			this.serialPort.writeString(timeSplit[2]+"\n");
			Thread.sleep(5000);
			buffer = this.serialPort.readString();
			this.writeLogFile(buffer);
			
			// for some reason before the refactor, there was another readString() in the SerialListener where the command
			// was being processed.  Either this was extra, or the serialPort sent back two strings.
			// TODO: figure out which is right.
			buffer = this.serialPort.readString();

		} catch (Exception e) {
			buffer = "Couldn't complete set clock. ";
			logger.warn(buffer, e);
			buffer = buffer + e.getMessage();
		}
		return buffer;
	}
	
	public String readClock() {
		String buffer = "";
		try {
			this.serialPort.writeString("44\n");
			Thread.sleep(5000);
			buffer = this.serialPort.readString();
		} catch (Exception e) {
			buffer = "Couldn't complete read clock. ";
			logger.warn(buffer, e);
			buffer = buffer + e.getMessage();
		}
		return buffer;
	}
	
	
	public boolean closePort() {
		SerialPort port = this.serialPort;
		String portAddress = port.getPortName();
		boolean result = false;
		try {
			port.removeEventListener();
			port.closePort();
			result = true;
//			portList.remove(portAddress);
//			logger.info("Removed port "+portAddress);
		}
		catch (SerialPortException ex) {
			logger.warn("Failed to close port "+portAddress);
			result = false;
		}
		return result;
	}

//	import java.nio.file.Paths;

//	import com.elyxor.xeros.ldcs.AppConfiguration;
//	Path dir = Paths.get(AppConfiguration.getLocalPath());
	//daiPrefix+daiNum+"log.txt";

	public void writeLogFile(String buffer) {

		//		Path dir = Paths.get(AppConfiguration.getLocalPath());
		//daiPrefix+daiNum+"log.txt";
		try {
			this._logWriter.write(daiPrefix + buffer);		
		} catch (IOException e) {
			logger.warn("Failed to write '" + buffer + "' to log file", e);
		}
    }
}
