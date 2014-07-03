package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.util.LogWriterInterface;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class WaterMeterPort implements DaiPortInterface, WaterMeterPortInterface {
	final static Logger logger = LoggerFactory.getLogger(WaterMeterPort.class);
	
	private long prevMeter1;
	private long prevMeter2;
	private SerialPort serialPort;
	private int daiNum;
	private LogWriterInterface logWriter;
	private String daiPrefix;
	private long waterMeterId;
	
	final static long defaultId = 999999999;
	final static int frontPadding = 5;
	final static int backPadding = 5;
	final static int idSize = 9;
	final static int idStartLocation = 4;
	final static int meterDataStartLocation = 203;
	final static int meterDataLength = 8;
	final static int responseLength = 255;
	
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
		boolean result;
		try {
			result = this.serialPort.openPort();
			this.serialPort.setParams(9600, 8, 1, 0);
		} catch (Exception ex) {
			logger.warn("Could not open port", ex);
			result = false;
		}		
    	logger.info("Opened water meter port " + this.serialPort.getPortName());
		return result;
	}

	public boolean closePort() {
		String portAddress = this.serialPort.getPortName();
		boolean result;
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
		byte[] buffer = null;
		byte[] request = createRequestString(this.parseIntToByteArray(defaultId));
		SerialPort port = this.getSerialPort();
		try {
//            port.writeIntArray(new int[]{0x2f, 0x3f, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x21, 0x0d, 0x0a});
            port.writeByte((byte)0x2f);
            port.writeByte((byte)0x3f);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x39);
            port.writeByte((byte)0x30);
            port.writeByte((byte)0x30);
            port.writeByte((byte)0x21);
            port.writeByte((byte)0x0d);
            port.writeByte((byte)0x0a);

//            port.writeBytes(request);
			Thread.sleep(5000);
			buffer = port.readBytes();
		} catch (Exception e) {
			String msg = "failed to send init request";
			logger.warn(msg, e);
		}
		this.setWaterMeterId(parseIdFromResponse(buffer));
		long[] meters = parseMetersFromResponse(buffer);
		this.setPrevMeters(meters[0], meters[1]);

        return buffer == null || buffer.length == 0 ? "" : buffer.toString();
    }

	public String sendRequest() {
		String result = "";
		byte[] buffer = null;
		byte[] requestBytes = this.createRequestString(parseIntToByteArray(this.getWaterMeterId()));
		SerialPort port = this.getSerialPort();
		long meter1;
		long meter2;
		long[] prevMeters = this.getPrevMeters();

		try {
			port.writeBytes(requestBytes);
			Thread.sleep(5000);
			buffer = port.readBytes(responseLength);
		} catch (Exception e) {
            String msg = "couldn't complete send request";
			logger.warn(msg, e);
			return msg + e;
		}
        if (buffer == null || buffer.length == 0) return result;

        logger.info("Captured log file");
        long[] meters = this.parseMetersFromResponse(buffer);
        meter1 = meters[0];
        meter2 = meters[1];

        result = "1,Std,\nFile Write Time: "
                + getSystemTime() + "\n"
                + "WM2: ,"
                + (meter1 - prevMeters[0]) + "\n"
                + "WM3: ,"
                + (meter2 - prevMeters[1]);
        this.setPrevMeters(meter1, meter2);
        return result;
	}
	
	public void writeLogFile(String buffer) {
		try {
			this.logWriter.write(this.daiPrefix + buffer);		
		} catch (IOException e) {
			logger.warn("Failed to write '" + buffer + "' to log file", e);
		}
		logger.info("Wrote log to file");
    }

	public void setPrevMeters (long meter1, long meter2) {
		this.prevMeter1 = meter1;
		this.prevMeter2 = meter2;
	}
	public long[] getPrevMeters () {
		return new long[] {prevMeter1, prevMeter2};
	}
	
	public long getWaterMeterId () {
		if (waterMeterId != 0) {
			return waterMeterId;
		}
		return -1;
	}
	public void setWaterMeterId (long id) {
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
	
	private long[] parseMetersFromResponse(byte[] response) {
		long[] result = new long[2];
		int[] meter1 = new int[meterDataLength];
		int[] meter2 = new int[meterDataLength];
		
		for (int i = 0; i < 16; i++) {
			if (i < 8) meter1[i] = response[meterDataStartLocation+i] - 48;
			meter2[i] = response[meterDataStartLocation+i] - 48;
		}
		result[0] = Long.parseLong(Arrays.toString(meter1));
		result[1] = Long.parseLong(Arrays.toString(meter2));
		return result;
	}
	private long parseIdFromResponse(byte[] response) {
		long result = 0;
		int[] id = new int[idSize];
		
		for (int i = 0; i < id.length; i++) {
            int value = response[idStartLocation+i];
            value = value > 0 ? value : value + 128;
			id[i] = value - 48;
		}
		result = Long.parseLong(Arrays.toString(id));
		return result;
	}
	private byte[] parseIntToByteArray (long in) {
		int i = idSize;
		byte[] idBytes = new byte[i];
		while (in > 0) {
			long digit = in % 10;
			in /= 10;
			idBytes[i-1] += ((byte) (0x00 + digit));
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
		for (int i = requestLength - backPadding; i > frontPadding; i--) {
			request[i-1] += id[idLength-1];
			idLength--;
		}
		return request;
	}
	private String getSystemTime() {
		SimpleDateFormat timingFormat = new SimpleDateFormat("hh:mm:ss");
        return timingFormat.format(System.currentTimeMillis());
	}

	//unused stubs
	public String getRemoteDaiId() {
		return null;
	}
	public String setRemoteDaiId(int id) {
		return null;
	}
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
	public String sendWaterRequest() {
		return null;
	}
	public SerialPortEventListener getSerialPortEventListener() {
		return null;
	}
	public void setSerialPortEventListener(SerialPortEventListener spel) {
	}
	public boolean ping() {
		return false;
	}
}
