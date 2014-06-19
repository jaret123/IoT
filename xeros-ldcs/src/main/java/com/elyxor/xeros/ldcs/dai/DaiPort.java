package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import com.elyxor.xeros.ldcs.HttpFileUploader;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;
import com.elyxor.xeros.ldcs.util.SerialReader;
import com.elyxor.xeros.ldcs.util.SerialReaderInterface;

public class DaiPort implements DaiPortInterface {

	final static Logger logger = LoggerFactory.getLogger(DaiPort.class);

	private SerialPort serialPort;
	private int daiNum;
	private String daiPrefix;
	private LogWriterInterface _logWriter;
	private SerialPortEventListener spel;
	
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
			
			SerialPortEventListener sri = new SerialReader(this);
			this.setSerialPortEventListener(sri);
			this.serialPort.addEventListener(sri);
			
			Thread.sleep(5000); //init time
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
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 12\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				buffer += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));

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
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 11\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				buffer += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			String msg = ("Couldn't complete send xeros request. ");
			logger.warn(msg, e);
			buffer = msg + e.getMessage(); 
		}
		return buffer;
	}
	
	public String sendWaterRequest() {
		String buffer = "";
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 13\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				buffer += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			String msg = ("Couldn't complete send water request. ");
			logger.warn(msg, e);
			buffer = msg + e.getMessage(); 
		}
		buffer = this.getDaiNum() + ", Std,\nFile Write Time: " + getSystemTime() + "\n" + buffer;
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
		int retryCounter = 0;
		try {
			this.serialPort.writeString("0 16\n");
			Thread.sleep(1000);
			buffer = this.serialPort.readString();
			while ((buffer == null || buffer.equals(" ") || buffer.equals("")) && retryCounter < 3) {
//				this.serialPort.writeString("0 16\n");
				Thread.sleep(500);
				buffer = this.serialPort.readString();
				retryCounter++;
			}
			String[] timeSplit = getSystemTime().split(":");
			
			this.serialPort.writeString(timeSplit[0]+"\n");
			Thread.sleep(500);
			buffer = this.serialPort.readString(); // clear buffer
			this.serialPort.writeString(timeSplit[1]+"\n");
			Thread.sleep(500);
			buffer = this.serialPort.readString(); // clear buffer
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
	
	public boolean ping() {
		int responseStatus = new HttpFileUploader().postPing(daiPrefix+this.getDaiNum());
		if (responseStatus == 200) {
			logger.info("successfully pinged server");
			return true;
		}
		else {
			logger.info("failed to ping server due to http response");
			return false;
		}
	}
	
	private String getSystemTime() {
		String result = "";
		SimpleDateFormat timingFormat = new SimpleDateFormat("kk:mm:ss");
		result = timingFormat.format(System.currentTimeMillis());
		return result;
	}

	public SerialPortEventListener getSerialPortEventListener() {
		return spel;
	}
	public void setSerialPortEventListener(SerialPortEventListener spel) {
		this.spel = spel;
	}

}
