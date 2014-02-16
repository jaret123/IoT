package com.elyxor.xeros.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_dai_meter_collection")
public class DaiMeterCollection {
	
	public DaiMeterCollection() {}

    private long id;
    private String machineType;
    private String machineName;
    private Timestamp collectionTime;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "machine_name")
	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	@Column(name = "timestamp")
	public Timestamp getCollectionTime() {
		return collectionTime;
	}

	public void setCollectionTime(Timestamp collectionTime) {
		this.collectionTime = collectionTime;
	}

	@Column(name = "machine_type")
	public String getMachineType() {
		return machineType;
	}

	public void setMachineType(String machineType) {
		this.machineType = machineType;
	}

	@Override
	public String toString() {
		return String.format("DaiMeterCollection [id=%s, machineType=%s, machineName=%s, collectionTime=%s]", 
				id, machineType, machineName, collectionTime);
	}
}
