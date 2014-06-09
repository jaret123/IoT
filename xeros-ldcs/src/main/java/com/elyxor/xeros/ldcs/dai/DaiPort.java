package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortException;

import com.elyxor.xeros.ldcs.util.FileLogWriter;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;
import com.elyxor.xeros.ldcs.util.SerialReader;
import com.elyxor.xeros.ldcs.util.SerialReaderInput;

public class DaiPort implements DaiPortInterface {

	final static Logger logger = LoggerFactory.getLogger(DaiPort.class);

	private SerialPort serialPort;
	private int daiNum;
	private String daiPrefix;
	private LogWriterInterface _logWriter;
	
//	static String test = "Str1 ,  Std ,\nFile Write Time: , 10 : 22 : 49\n"
//			+ "0 ,, 00 : 00 : 00  ,  0 : 0 : 0.0  ,,  00 : 00 : 00  ,  0 : 0 : 0.0  ,,"
//			+ "  00 : 00 : 00  ,  0 : 0 : 0.0  ,,  00 : 00 : 00  ,  0 : 0 : 0.0  ,, "
//			+ " 00 : 00 : 00  ,  0 : 0 : 0.0  ,,  00 : 00 : 00  ,  0 : 0 : 0.0  ,,  "
//			+ "00 : 00 : 00  ,  0 : 0 : 0.0  ,,  00 : 00 : 00  ,  0 : 0 : 0.0  ,,\n"
//			+ "WM 0:  , 355 , 1291 , 355\nWM 1:  , 0 , 600 , 0 \0x04\0x04\0x04";		
//
//	public static void main(String[] args) {
//		DaiPortInterface daiPort = new DaiPort(new SerialPort("test"), 1, new FileLogWriter(Paths.get("/home/will/ldcs/input"), "Str1"+"Log.txt"), "Str");
//		daiPort.writeLogFile(test);
//	}
	public DaiPort (SerialPort port, int num, LogWriterInterface logWriter, String prefix) { 
		this.serialPort = port;
		this.daiNum = num;
		this._logWriter = logWriter;
		this.daiPrefix = prefix;
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
	
	
	public boolean openPort() {
		boolean result = false;
		try {
			result = this.serialPort.openPort();
			this.serialPort.setParams(4800, 7, 1, 2, false, false);
			this.serialPort.setEventsMask(SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR);
//			this.serialPort.setFlowControlMode(4 | 8);
			this.serialPort.addEventListener(new SerialReader(this));
			
	    	logger.info("Started listening on port " + this.serialPort.getPortName());
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
			logger.warn(msg , e);
			result = msg + e.getMessage();
		}
		return result;
	}
	
	public String getRemoteDaiId() {
		String daiId = "";
		try {
			this.serialPort.writeString("0 19\n");
			Thread.sleep(1000);
			daiId = this.serialPort.readString();
			Thread.sleep(4000);
		}
		catch (Exception e) {
			logger.warn("Couldn't read dai id", e);
		}
		return daiId;
	}
	
	public String setRemoteDaiId(int id) {
		String daiId = "";
		try {
			this.serialPort.writeString("0 19\n");
			Thread.sleep(1000);
			this.serialPort.readString(); //clear buffer
			this.serialPort.writeString(id+"\n");
			Thread.sleep(1000);
			daiId = this.serialPort.readString();
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
			this.serialPort.writeByte((byte)0x13);
			this.serialPort.writeString("0 12\r\n");
			this.serialPort.writeByte((byte)0x11);
			Thread.sleep(10000);
			buffer += this.serialPort.readString();
		} catch (Exception e) {
			String msg = "Couldn't complete send std request. ";
			logger.warn(msg, e);
			buffer = msg + e.getMessage(); 
		}
		return buffer;
	}
	
	public String sendXerosRequest() {
		String buffer = "";
		try {
			this.serialPort.writeByte((byte)0x11);
			this.serialPort.writeString("0 11\n\r");
			this.serialPort.writeByte((byte)0x13);
			int bufferSize = 1;
			Thread.sleep(500);
			while (bufferSize > 0) {
				bufferSize = this.serialPort.getInputBufferBytesCount();
				logger.info("buffer size: "+bufferSize);
				buffer += this.serialPort.readString(bufferSize);
				Thread.sleep(500);
			}
			logger.info(buffer);
		} catch (Exception e) {
			String msg = ("Couldn't complete send xeros request");
			logger.warn(msg, e);
			buffer = msg + e.getMessage(); 
		}
		return buffer;
	}
	
	public String sendRequest() {
		String buffer = "";	    		
		try {
			this.serialPort.writeString("0 999\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				buffer += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
		} catch (Exception e) {
			logger.warn("Couldn't complete send request", e);
		}
		if (!buffer.equals("")) logger.info("Captured log file");
		return buffer;
	}	
	
	public String setClock() {
		String buffer = "";
		try {
			this.serialPort.writeString("0 16\n");
			this.serialPort.readString(); // clear buffer
			SimpleDateFormat timingFormat = new SimpleDateFormat("hh:mm:ss");
			buffer = timingFormat.format(System.currentTimeMillis());
			String[] timeSplit = buffer.split(":");
			
			this.serialPort.writeString(timeSplit[0]+"\n");
			this.serialPort.readString(); // clear buffer
			this.serialPort.writeString(timeSplit[1]+"\n");
			this.serialPort.readString(); // clear buffer
			this.serialPort.writeString(timeSplit[2]+"\n");
			Thread.sleep(5000);
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
			this.serialPort.writeString("0 15\n");
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
			
	public void writeLogFile(String buffer) {
		if (buffer != null) {
			String[] bufferSplit = buffer.split(",");
			String logPrefix = "";
			if (1 < bufferSplit.length) logPrefix = bufferSplit[1].trim();
			
			LogWriterInterface writer = new FileLogWriter(Paths.get(this._logWriter.getPath()), logPrefix+"-"+this._logWriter.getFilename());
			try {
				writer.write(this.daiPrefix + buffer);		
			} catch (IOException e) {
				logger.warn("Failed to write '" + buffer + "' to log file", e);
			}
			logger.info("Wrote log to file");
		}
    }
}
