package com.elyxor.xeros.ldcs.dai;

public interface DaiPortInterface {

	// initialization and cleanup
	public boolean openPort();
	public boolean closePort();

	// commands
	public String clearPortBuffer();
	public String readClock();
	public String setClock();
	public String setRemoteDaiId(int id);
	public String sendStdRequest();
	public String sendXerosRequest();

}
