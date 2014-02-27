package com.elyxor.xeros.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_collection_map_detail")
public class CollectionClassificationMapDetail {

	int id;
	CollectionClassificationMap collectionClassificationMap;
	String meterType;
	float duration;
	float startTime;

    @Id
    @Column(name = "id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", referencedColumnName = "collection_map_id")	
	public CollectionClassificationMap getCollectionClassificationMap() {
		return collectionClassificationMap;
	}

	public void setCollectionClassificationMap(
			CollectionClassificationMap collectionClassificationMap) {
		this.collectionClassificationMap = collectionClassificationMap;
	}

	
	@Column(name = "meter_type")
	public String getMeterType() {
		return meterType;
	}

	public void setMeterType(String meterType) {
		this.meterType = meterType;
	}

	@Column(name = "duration", scale=10, precision=2)
	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	@Column(name = "start_time", scale=10, precision=2)
	public float getStartTime() {
		return startTime;
	}

	public void setStartTime(float startTime) {
		this.startTime = startTime;
	}
	
	public boolean matches(CollectionClassificationMapDetail other) {
		
		boolean rtn = (this.meterType.equals(other.getMeterType()) && this.duration == other.getDuration());
		if (rtn && 	this.duration > 0) {
			rtn = rtn && (this.startTime == other.getStartTime() );
		}
		return rtn;
	}
	
	@Override
	public String toString() {
		return String.format("CollectionClassificationMapDetail [meterType=%s, duration=%s, startTime=%s]", meterType, duration, startTime);
	}
	
}
