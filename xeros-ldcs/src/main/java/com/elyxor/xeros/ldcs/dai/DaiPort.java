package com.elyxor.xeros.ldcs.dai;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortException;

import com.elyxor.xeros.ldcs.HttpFileUploader;
import com.elyxor.xeros.ldcs.util.FileLogWriter;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;
import com.elyxor.xeros.ldcs.util.SerialReader;

public class DaiPort implements DaiPortInterface {

	final static Logger logger = LoggerFactory.getLogger(DaiPort.class);

	private SerialPort serialPort;
	private int daiNum;
	private String daiPrefix;
	private LogWriterInterface _logWriter;
	
	public DaiPort (SerialPort port, int num, LogWriterInterface logWriter, String prefix) { 
		this.serialPort = port;
		this.daiNum = num;
		this._logWriter = logWriter;
		this.daiPrefix = prefix;
	}
		
	public boolean openPort() {
		boolean result = false;
		try {
			result = this.serialPort.openPort();
			this.serialPort.setParams(4800, 7, 1, 2, false, false); //specific serial port parameters for Xeros DAQ
			this.serialPort.addEventListener(new SerialReader(this));
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
		String result = "";
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 19\n");
			Thread.sleep(1000);
			result = this.serialPort.readString();
			Thread.sleep(4000); //let the DAQ timeout to avoid setting a new id accidentally
			this.serialPort.addEventListener(new SerialReader(this));
		}
		catch (Exception e) {
			logger.warn("Couldn't read dai id", e);
		}
		return result;
	}
	
	public String setRemoteDaiId(int id) {
		String result = "";
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 19\n");
			Thread.sleep(1000);
			this.serialPort.readString(); //clear buffer, DAQ writes old daiId
			this.serialPort.writeString(id+"\n");
			Thread.sleep(1000);
			result = this.serialPort.readString();
			this.serialPort.addEventListener(new SerialReader(this));
		}
		catch (Exception e) {
			logger.warn("Couldn't set port id", e);
		}
		this.setDaiNum(id);
		return result;
	}
	
	public String sendStdRequest() {
		String result = "";
		
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 12\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));

		} catch (Exception e) {
			String msg = "Couldn't complete send std request. ";
			logger.warn(msg, e);
			result = msg + e.getMessage(); 
		}
		return result;
	}
	
	public String sendXerosRequest() {
		String result = "";
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 11\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			String msg = ("Couldn't complete send xeros request. ");
			logger.warn(msg, e);
			result = msg + e.getMessage(); 
		}
		return result;
	}
	
	public String sendWaterRequest() throws Exception {
		String result = "";
        this.serialPort.removeEventListener();
        this.serialPort.writeString("0 13\n");
        Thread.sleep(1000);
        while (this.serialPort.getInputBufferBytesCount() > 0) {
            result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
            Thread.sleep(500);
        }
        String time = this.getSystemTime();
        if (time.startsWith("00")) {
            this.serialPort.writeString("0 116\n");
            Thread.sleep(500);
            this.serialPort.writeString("0 116\n");
            Thread.sleep(500);
            this.serialPort.writeString("0 116\n");
        }
        this.serialPort.addEventListener(new SerialReader(this));

		result = this.getDaiNum() + ", Std,\nFile Write Time: , " + time + "\n" + result;
		return result;
	}

	public String sendRequest() {
		String result = "";	    		
		try {
			this.serialPort.writeString("0 999\n");
			Thread.sleep(1000);
			while (this.serialPort.getInputBufferBytesCount() > 0) {
				result += this.serialPort.readString(this.serialPort.getInputBufferBytesCount());
				Thread.sleep(500);
			}
		} catch (Exception e) {
			logger.warn("Couldn't complete send request", e);
		}
		if (!result.equals("")) logger.info("Captured log file");
		return result;
	}	
		
	public String setClock() {
        String result = "";
        try {
            this.serialPort.removeEventListener();
            this.serialPort.writeString("0 16\n");
            String[] timeSplit = this.getSystemTime().replaceAll(" ","").split(":");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[0] + "\n");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[1] + "\n");
            Thread.sleep(100);
            this.serialPort.writeString(timeSplit[2] + "\n");
            Thread.sleep(500);
            result = this.serialPort.readString();
            this.serialPort.addEventListener(new SerialReader(this));
        } catch (Exception e) {
            result = "Couldn't complete set clock. ";
            logger.warn(result, e);
            result = result + e.getMessage();
        }
        return result;
    }

	public String readClock() {
		String result = "";
		try {
			this.serialPort.removeEventListener();
			this.serialPort.writeString("0 15\n");
			Thread.sleep(5000);
			result = this.serialPort.readString();
			
			this.serialPort.addEventListener(new SerialReader(this));
		} catch (Exception e) {
			result = "Couldn't complete read clock. ";
			logger.warn(result, e);
			result = result + e.getMessage();
		}
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
			
	public void writeLogFile(String buffer) {
		if (buffer != null) {
			String[] bufferSplit = buffer.split(",");
			String logPrefix = "";
			if (1 < bufferSplit.length) logPrefix = bufferSplit[1].trim();
			
			LogWriterInterface writer = new FileLogWriter(this._logWriter.getPath().getParent(), logPrefix+"-"+this._logWriter.getFilename());
			try {
				writer.write(this.daiPrefix + buffer);		
			} catch (IOException e) {
				logger.warn("Failed to write '" + buffer + "' to log file", e);
			}
			logger.info("successfully sent log to filewriter");
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
        SimpleDateFormat timingFormat = new SimpleDateFormat("kk : mm : ss dd-MM-yyyy");
        result = timingFormat.format(System.currentTimeMillis());
        return result;
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
}
