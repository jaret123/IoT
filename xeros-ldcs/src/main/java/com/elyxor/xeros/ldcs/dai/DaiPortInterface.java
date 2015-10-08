package com.elyxor.xeros.ldcs.dai;

import com.elyxor.xeros.ldcs.thingworx.XerosWasherThing;
import com.elyxor.xeros.ldcs.util.LogWriterInterface;
import jssc.SerialPort;

public interface DaiPortInterface {

	// parameter values
	public SerialPort getSerialPort();
	public void setSerialPort(SerialPort port);
	
	public int getDaiNum();
	public void setDaiNum(int num);

    public LogWriterInterface getLogWriter();
    public void setLogWriter(LogWriterInterface lwi);

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
	public String sendWaterRequest() throws Exception;
    public String sendXerosWaterRequest() throws Exception;

    public String sendRequest();
    public String initWaterRequest();
    public long[] calculateWaterLog(String buffer);
    public void writeWaterOnlyLog(long[] meters);
    void writeWaterOnlyXerosLog(long[] result);
    public String getConfig();

    public boolean sendMachineStatus();

	//utilities
	public void writeLogFile(String s);
	public boolean ping();

    void setXerosWasherThing(XerosWasherThing thing);

    String initXerosWaterRequest();
    long[] calculateXerosWaterLog(String buffer);
}
