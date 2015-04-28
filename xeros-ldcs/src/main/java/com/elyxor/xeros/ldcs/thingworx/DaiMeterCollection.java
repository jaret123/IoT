package com.elyxor.xeros.ldcs.thingworx;

import java.sql.Timestamp;
import java.util.Collection;

public class DaiMeterCollection {
	
	public DaiMeterCollection() {}

    private int id;
    private String machineIdentifier;
    private String locationIdentifier;
    private String daiIdentifier;

    private String olsonTimezoneId;
    private Timestamp daiCollectionTime;
    private Timestamp fileCreateTime;
    private Timestamp fileUploadTime;

    private Collection<DaiMeterCollectionDetail> collectionDetails;

    private float earliestValue;
    private Integer exception;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMachineIdentifier() {
		return machineIdentifier;
	}

	public void setMachineIdentifier(String machineIdentifier) {
		this.machineIdentifier = machineIdentifier;
	}

	public Timestamp getDaiCollectionTime() {
		return daiCollectionTime;
	}

	public void setDaiCollectionTime(Timestamp daiCollectionTime) {
		this.daiCollectionTime = daiCollectionTime;
	}

	public Timestamp getFileUploadTime() {
		return fileUploadTime;
	}

	public void setFileUploadTime(Timestamp fileUploadTime) {
		this.fileUploadTime = fileUploadTime;
	}


	public String getOlsonTimezoneId() {
		return olsonTimezoneId;
	}

	public void setOlsonTimezoneId(String olsonTimezoneId) {
		this.olsonTimezoneId = olsonTimezoneId;
	}

	public Timestamp getFileCreateTime() {
		return fileCreateTime;
	}

	public void setFileCreateTime(Timestamp fileCreateTime) {
		this.fileCreateTime = fileCreateTime;
	}

	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	public String getDaiIdentifier() {
		return daiIdentifier;
	}

	public void setDaiIdentifier(String daiIdentifier) {
		this.daiIdentifier = daiIdentifier;
	}

	public Collection<DaiMeterCollectionDetail> getCollectionDetails() {
		return collectionDetails;
	}

	public void setCollectionDetails(Collection<DaiMeterCollectionDetail> collectionDetails) {
		this.collectionDetails = collectionDetails;
	}
	public float getEarliestValue() {
		return this.earliestValue;
	}
	public void setEarliestValue(float value) {
		this.earliestValue = value;
	}

	@Override
	public String toString() {
		return String.format("DaiMeterCollection [id=%s, dai=%s, machine=%s, collectionTime=%s]", id, daiIdentifier, machineIdentifier, daiCollectionTime);
	}
}
