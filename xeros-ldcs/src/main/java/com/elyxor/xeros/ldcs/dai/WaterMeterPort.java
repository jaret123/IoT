package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import com.elyxor.xeros.ldcs.util.FileLogWriter;
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
	private String waterMeterId;
	
	final static String defaultId = "999999999999";
	final static int frontPadding = 2;
	final static int backPadding = 5;
	final static int idSize = 12;
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
		byte[] request = createRequestString(this.parseStringToByteArray(defaultId));
		SerialPort port = this.getSerialPort();
		try {
            port.writeBytes(request);
			Thread.sleep(500);
			buffer = port.readBytes();
		} catch (Exception e) {
			String msg = "failed to send init request";
			logger.warn(msg, e);
		}
		this.setWaterMeterId(parseIdFromResponse(buffer));
		long[] meters = parseMetersFromResponse(buffer);
        this.storePrevMeters(meters);
		this.setPrevMeters(meters[0],meters[1]);

        return buffer == null || buffer.length == 0 ? "" : buffer.toString();
    }

    private void storePrevMeters(long[] meters) {
        FileLogWriter writer = new FileLogWriter(this.logWriter.getPath().resolve("/waterMeters"),  daiPrefix + "meterBuffer.txt");
        for (int i = 0; i < meters.length; i++) {
            try {
                writer.write("meter"+i+","+meters[i] +","+ getSystemTime());
                logger.info("successfully stored previous meters in log file");
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("failed to store previous meters",e);
            }
        }
    }

    public String sendRequest() {
		String result = "";
		byte[] buffer = null;
		byte[] request = this.createRequestString(this.parseStringToByteArray(this.getWaterMeterId()));
		SerialPort port = this.getSerialPort();
		long meter1;
		long meter2;
		long[] prevMeters = this.getPrevMeters();

		try {
			port.writeBytes(request);
			Thread.sleep(500);
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

        result = this.getDaiNum() + " , Std , \nFile Write Time: , "
                + getSystemTime() + "\n"
                + "WM2: , 0 , 0 , "
                + (meter1 - prevMeters[0]) + "\n"
                + "WM3: , 0 , 0 , "
                + (meter2 - prevMeters[1]);
        this.setPrevMeters(meter1, meter2);
        this.storePrevMeters(meters);
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
	public String getWaterMeterId () {
		if (!(waterMeterId == null)) {
			return waterMeterId;
		}
		return "";
	}
	public void setWaterMeterId (String id) {
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
		String meter1 = "";
		String meter2 = "";
		
		for (int i = 0; i < 16; i++) {
            int value = response[meterDataStartLocation+i];
            value = value > 0 ? value : value + 128;
			if (i < 8) meter1 += value - 48;
			else meter2 += value - 48;
		}
		result[0] = Long.parseLong(meter1);
		result[1] = Long.parseLong(meter2);
		return result;
	}
	private String parseIdFromResponse(byte[] response) {
		String result = "";
		int[] id = new int[idSize];
		
		for (int i = 0; i < id.length; i++) {
            int value = response[idStartLocation+i];
            value = value > 0 ? value : value + 128;
			result += value - 48;
		}
		return result;
	}
    private byte[] parseStringToByteArray(String in) {
        int i = idSize;
        byte[] idBytes = new byte[i];
        while (i > 0) {
            idBytes[i-1] = (byte) (0x00 + in.charAt(i-1));
            i--;
        }
        return idBytes;
    }
	private byte[] createRequestString (byte[] id) {
		byte[] request = Arrays.copyOf(requestBytes, requestBytes.length);
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
		SimpleDateFormat timingFormat = new SimpleDateFormat("hh : mm : ss dd-MM-yyyy");
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
	public boolean ping() {
		return false;
	}
}
