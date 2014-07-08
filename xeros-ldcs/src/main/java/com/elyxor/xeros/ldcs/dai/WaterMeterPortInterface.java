package com.elyxor.xeros.ldcs.dai;

public interface WaterMeterPortInterface extends DaiPortInterface {
	
	//parameters
	public void setPrevMeters(long meter1, long meter2);
	public long[] getPrevMeters();

	public String getWaterMeterId();
	public void setWaterMeterId(String id);

	//commands
	public String initRequest();
}