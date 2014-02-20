package com.elyxor.xeros.model;


import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "xeros_dai_meter_collection_detail")
public class DaiMeterCollectionDetail {

    private int id;    
    private String meterType;
    private float meterValue;
    private Timestamp timestamp;
    private float duration;
    private DaiMeterCollection daiMeterCollection;
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public DaiMeterCollectionDetail() {}


    @Id
    @Column(columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", referencedColumnName = "id")
	public DaiMeterCollection getDaiMeterCollection() {
		return daiMeterCollection;
	}


	public void setDaiMeterCollection(DaiMeterCollection daiMeterCollection) {
		this.daiMeterCollection = daiMeterCollection;
	}


	@NotNull
	@Column(name = "meter_type")
	public String getMeterType() {
		return meterType;
	}

	public void setMeterType(String meterType) {
		this.meterType = meterType;
	}


	@NotNull
	@Column(name = "meter_value")	
	public float getMeterValue() {
		return meterValue;
	}

	public void setMeterValue(float meterValue) {
		this.meterValue = meterValue;
	}

	@NotNull
	@Column(name = "timestamp")	
	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}


	@Column(name = "duration")	
	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	@Override
    public String toString() {
        return String.format("DAIMeterDetail[id=%d, time='%s', type='%s']", 
        		id, sdf.format(new Date(timestamp.getTime())), meterType);
    }

}
