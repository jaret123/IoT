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
@Table(name = "xeros_machine")
public class Machine {

	public Machine() {}
	
	private int id;
	private String serialNumber;
	private String manufacturer;
	private int steam;
	private int fuel_type;
	private Location location;
	private String machineType;
	private String hotWaterMeterType;
	private String coldWaterMeterType;
	private String doorLockMeterType;
	private int startTimeOffset;
	private int stopTimeOffset;
	
	
    @Id
    @Column(name = "machine_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="serial_number", length=255)
	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	@Column(name="manufacturer", length=255)
	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	@Column(name = "steam", scale=10)
	public int getSteam() {
		return steam;
	}

	public void setSteam(int steam) {
		this.steam = steam;
	}

	@Column(name = "fuel_type", scale=10)
	public int getFuel_type() {
		return fuel_type;
	}

	public void setFuel_type(int fuel_type) {
		this.fuel_type = fuel_type;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", referencedColumnName = "location_id")
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	@Column(name="machine_type")
	public String getMachineType() {
		return machineType;
	}

	public void setMachineType(String machineType) {
		this.machineType = machineType;
	}

	@Column(name="hot_water_meter_type", length=32)
	public String getHotWaterMeterType() {
		return hotWaterMeterType;
	}

	public void setHotWaterMeterType(String hotWaterMeterType) {
		this.hotWaterMeterType = hotWaterMeterType;
	}

	@Column(name="cold_water_meter_type", length=32)
	public String getColdWaterMeterType() {
		return coldWaterMeterType;
	}

	public void setColdWaterMeterType(String coldWaterMeterType) {
		this.coldWaterMeterType = coldWaterMeterType;
	}

	@Column(name="door_lock_meter_type", length=32)
	public String getDoorLockMeterType() {
		return doorLockMeterType;
	}

	public void setDoorLockMeterType(String doorLockMeterType) {
		this.doorLockMeterType = doorLockMeterType;
	}

	@Column(name = "start_time_offset", scale=10)
	public int getStartTimeOffset() {
		return startTimeOffset;
	}

	public void setStartTimeOffset(int startTimeOffset) {
		this.startTimeOffset = startTimeOffset;
	}

	@Column(name = "stop_time_offset", scale=10)
	public int getStopTimeOffset() {
		return stopTimeOffset;
	}

	public void setStopTimeOffset(int stopTimeOffset) {
		this.stopTimeOffset = stopTimeOffset;
	}

	

	
}
