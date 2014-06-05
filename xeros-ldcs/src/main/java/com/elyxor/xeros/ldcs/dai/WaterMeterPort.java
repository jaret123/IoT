package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.util.LogWriterInterface;

import jssc.SerialPort;
import jssc.SerialPortException;

public class WaterMeterPort implements DaiPortInterface, WaterMeterPortInterface {
	final static Logger logger = LoggerFactory.getLogger(WaterMeterPort.class);
	
	private float prevMeter1;
	private float prevMeter2;
	private SerialPort serialPort;
	private int daiNum;
	private LogWriterInterface logWriter;
	private String daiPrefix;
	private int waterMeterId;
	
	final static int defaultId = 999999999;
	final static int frontPadding = 2;
	final static int backPadding = 5;
	final static int idSize = 12;
	final static int meterDataStartLocation = 83;
	final static int meterDataLength = 8;
	
	final static byte[] requestBytes = {(byte)0x2f,(byte)0x3f,(byte)0x30,(byte)0x30,(byte)0x30,
			(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30,
			(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x21,(byte)0x0d,(byte)0x0a}; 
	
	public WaterMeterPort(SerialPort sp, int num, LogWriterInterface lwi, String prefix) {
		this.serialPort = sp;
		this.daiNum = num;
		this.logWriter = lwi;
		this.daiPrefix = prefix;
	}
	
	public boolean openPort() {
		boolean result = false;
		try {
			result = this.serialPort.openPort();
			this.serialPort.setParams(9600, 8, 1, 0, false, false);														
		} catch (Exception ex) {
			logger.warn("Could not open port", ex);
			result = false;
		}
		if (result) {
			
		}
		
    	logger.info("Opened water meter port " + this.serialPort.getPortName());
    	
		return result;
	}
	public boolean closePort() {
		String portAddress = this.serialPort.getPortName();
		boolean result = false;
		try {
			this.serialPort.removeEventListener();
			result = this.serialPort.closePort();
		}
		catch (SerialPortException ex) {
			logger.warn("Failed to close port "+portAddress);
			result = false;
		}
		return result;
	}
	
	public String initRequest() {
		String result = "";
		byte[] buffer = null;
		
		byte[] request = createRequestString(this.parseIntToByteArray(defaultId));
		try {
			this.serialPort.writeBytes(request);
			buffer = this.serialPort.readBytes();
		} catch (SerialPortException e) {
			String msg = "failed to send init request";
			logger.warn(msg, e);
		}
		return buffer.toString();
	}

	public String sendRequest() {
		String result = "";
		byte[] buffer = null;
		byte[] requestBytes = {(byte)0x04, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
				(byte)0x08, (byte)0x44, (byte)0x59};
		int meter1 = 0;
		int meter2 = 0;
		
		try {
			this.serialPort.writeBytes(requestBytes);
			Thread.sleep(10000);
			buffer = this.serialPort.readBytes(21);
		} catch (Exception e) {
			logger.warn("Couldn't complete send request", e);
			result = buffer.toString();
		}
		if (buffer!=null) {
			logger.info("Captured log file");
			meter1 = buffer[6] | buffer[5] << 8 | buffer[4] << 16 | buffer[3] << 32;
			meter2 = buffer[10] | buffer[9] << 8 | buffer[8] << 16 | buffer[7] << 32;
			
			result = "1,Std,\nFile Write Time: "
					+ getSystemTime() + "\n"
					+ "WM2: ,"
					+ (meter1 - prevMeter1) + "\n"
					+ "WM3: ,"
					+ (meter2 - prevMeter2);
			prevMeter1 = meter1;
			prevMeter2 = meter2;
		}		
		return result;
	}

	public String getRemoteDaiId() {
		return null;
	}
	public String setRemoteDaiId(int id) {
		return null;
	}
		
	public void writeLogFile(String buffer) {
		try {
			this.logWriter.write(this.daiPrefix + buffer);		
		} catch (IOException e) {
			logger.warn("Failed to write '" + buffer + "' to log file", e);
		}
		logger.info("Wrote log to file");
    }

	public void setPrevMeters (float meter1, float meter2) {
		this.prevMeter1 = meter1;
		this.prevMeter2 = meter2;
	}
	public float[] getPrevMeters () {
		return new float[] {prevMeter1, prevMeter2};
	}
	
	public int getWaterMeterId () {
		if (waterMeterId != 0) {
			return waterMeterId;
		}
		return -1;
	}
	public void setWaterMeterId (int id) {
		this.waterMeterId = id;
	}
	
	public SerialPort getSerialPort() {
		if (serialPort != null) {
			return this.serialPort;
		}
		return null;
	}
	public void setSerialPort(SerialPort port) {
		this.serialPort = port;
	}
	
	public int getDaiNum() {
		if (daiNum!=0) {
			return this.daiNum;
		}
		return -1;
	}
	public void setDaiNum(int num) {
		this.daiNum = num;
	}
	
	private float[] parseMetersFromResponse(byte[] response) {
		
	}
	
	private byte[] parseIntToByteArray (int in) {
		int i = idSize;
		byte[] idBytes = new byte[i];
		while (in > 0) {
			int digit = in % 10;
			in /= 10;
			idBytes[i] += ((byte) (0x00 + digit));
			i--;
		}
		return idBytes;
	}
	private byte[] createRequestString (byte[] id) {
		byte[] request = requestBytes;
		int idLength = id.length;
		int requestLength = request.length;
		if (idLength + frontPadding + backPadding != requestLength) {
			String msg = "id size does not fit into request size";
			logger.warn(msg);
			return request;
		}
		for (int i = requestLength - backPadding; i >= frontPadding; i--) {
			request[i] += id[idLength];
			idLength--;
		}
		return request;
	}
	
	private String getSystemTime() {
		SimpleDateFormat timingFormat = new SimpleDateFormat("hh:mm:ss");
		String currentTime = timingFormat.format(System.currentTimeMillis());
		return currentTime;
	}

	//unused stubs
	public String clearPortBuffer() {
		return null;
	}
	public String readClock() {
		return null;
	}
	public String setClock() {
		return null;
	}
	public String sendStdRequest() {
		return null;
	}
	public String sendXerosRequest() {
		return null;
	}
}
