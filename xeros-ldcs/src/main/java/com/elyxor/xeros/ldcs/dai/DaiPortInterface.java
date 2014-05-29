package com.elyxor.xeros.ldcs.dai;

import jssc.SerialPort;

public interface DaiPortInterface {

	// parameter values
	public SerialPort getSerialPort();
	public void setSerialPort(SerialPort port);
	
	public int getDaiNum();
	public void setDaiNum(int num);
	
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

}
