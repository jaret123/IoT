package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.util.SerialReaderInterface;

import jssc.SerialPort;
import jssc.SerialPortEventListener;

public interface DaiPortInterface {

	// parameter values
	public SerialPort getSerialPort();
	public void setSerialPort(SerialPort port);
	
	public int getDaiNum();
	public void setDaiNum(int num);
	
	public SerialPortEventListener getSerialPortEventListener();
	public void setSerialPortEventListener(SerialPortEventListener spel);
	
	// initialization and cleanup
	public boolean openPort();
	public boolean closePort();

	// commands
	public String clearPortBuffer();
	public String readClock();
	public String setClock();
	public String getRemoteDaiId();
	public String setRemoteDaiId(int id);
	public String sendStdRequest();
	public String sendXerosRequest();
	public String sendWaterRequest();
	public String sendRequest();
	
	//utilities
	public void writeLogFile(String s);
	public boolean ping();
}
