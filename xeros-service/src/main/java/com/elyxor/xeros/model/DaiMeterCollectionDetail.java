package com.elyxor.xeros.model;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Table(name = "xeros_dai_meter_collection_detail")
public class DaiMeterCollectionDetail {

    private int id;
    private String meterType;
    private Float meterValue;
    private Timestamp timestamp;
    private Float duration;
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
	@Column(name = "meter_value", scale=10, precision=2)	
	public Float getMeterValue() {
		return meterValue;
	}

	public void setMeterValue(Float meterValue) {
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


	@Column(name = "duration", scale=10, precision=2)	
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
