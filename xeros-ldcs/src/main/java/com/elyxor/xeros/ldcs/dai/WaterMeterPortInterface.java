package com.elyxor.xeros.ldcs.dai;

public interface WaterMeterPortInterface {
	
	//parameters
	public void setPrevMeters(long meter1, long meter2);
	public long[] getPrevMeters();

	public long getWaterMeterId();
	public void setWaterMeterId(int id);

	//commands
	public String initRequest();
}