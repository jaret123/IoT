package com.elyxor.xeros.ldcs.thingworx;


import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DaiMeterCollectionDetail {

    private int id;
    private String meterType;
    private Float meterValue;
    private Timestamp timestamp;
    private Float duration;
    private DaiMeterCollection daiMeterCollection;
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public DaiMeterCollectionDetail() {}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public DaiMeterCollection getDaiMeterCollection() {
		return daiMeterCollection;
	}


	public void setDaiMeterCollection(DaiMeterCollection daiMeterCollection) {
		this.daiMeterCollection = daiMeterCollection;
	}


	public String getMeterType() {
		return meterType;
	}

	public void setMeterType(String meterType) {
		this.meterType = meterType;
	}


	public Float getMeterValue() {
		return meterValue;
	}

	public void setMeterValue(Float meterValue) {
		this.meterValue = meterValue;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public Float getDuration() {
		return duration;
	}

	public void setDuration(Float duration) {
		this.duration = duration;
	}

	@Override
    public String toString() {
        return String.format("DAIMeterDetail[id=%d, time='%s', type='%s']", 
        		id, sdf.format(new Date(timestamp.getTime())), meterType);
    }

}
