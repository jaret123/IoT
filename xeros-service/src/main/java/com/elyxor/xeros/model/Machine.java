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
	private Integer steam;
	private Integer fuel_type;
	private Location location;
	private String machineIdentifier;
	private String machineType;
	private String hotWaterMeterType;
	private String coldWaterMeterType;
	private String doorLockMeterType;
	private String ignoreMeterType;
	private Integer unknownClass;
	private Integer startTimeOffset;
	private Integer stopTimeOffset;
	private Integer doorLockDurationMatchVariance;
	private Integer sensorStartTimeVariance;
	private Integer durationMatchVariance;
	
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

	@Column(name = "door_lock_duration_match_variance", scale=10)
	public Integer getDoorLockDurationMatchVariance() {
		return doorLockDurationMatchVariance;
	}

	public void setDoorLockDurationMatchVariance(Integer doorLockDurationMatchVariance) {
		this.doorLockDurationMatchVariance = doorLockDurationMatchVariance;
	}

	@Column(name = "sensor_start_match_variance", scale=10)
	public Integer getSensorStartTimeVariance() {
		return sensorStartTimeVariance;
	}

	public void setSensorStartTimeVariance(Integer sensorStartTimeVariance) {
		this.sensorStartTimeVariance = sensorStartTimeVariance;
	}

	public void setSteam(Integer steam) {
		this.steam = steam;
	}

	public void setFuel_type(Integer fuel_type) {
		this.fuel_type = fuel_type;
	}

	@Column(name="machine_identifier", length=255)
	public String getMachineIdentifier() {
		return machineIdentifier;
	}

	public void setMachineIdentifier(String machineIdentifier) {
		this.machineIdentifier = machineIdentifier;
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
	public Integer getStartTimeOffset() {
		return startTimeOffset;
	}

	public void setStartTimeOffset(Integer startTimeOffset) {
		this.startTimeOffset = startTimeOffset;
	}

	@Column(name = "stop_time_offset", scale=10)
	public Integer getStopTimeOffset() {
		return stopTimeOffset;
	}

	public void setStopTimeOffset(Integer stopTimeOffset) {
		this.stopTimeOffset = stopTimeOffset;
	}

	
	@Override
	public String toString() {
		return String
				.format("Machine [id=%s, manufacturer=%s, machineIdentifier=%s, machineType=%s]",
						id, manufacturer, machineIdentifier, machineType);
	}
	
	@Column (name = "duration_match_variance", scale=10)
	public Integer getDurationMatchVariance() {
		return durationMatchVariance;
	}

	public void setDurationMatchVariance(Integer durationMatchVariance) {
		this.durationMatchVariance = durationMatchVariance;
	}
	//setter and getter for ignore meter type
	@Column (name = "ignore_meter_type", scale=10)
	public String getIgnoreMeterType() {
		return ignoreMeterType;
	}

	public void setIgnoreMeterType(String ignoreMeterType) {
		this.ignoreMeterType = ignoreMeterType;
	}
	
	//setter and getter for unknown collection map
	@Column (name = "unknown_class", scale=10)
	public Integer getUnknownClass() {
		return unknownClass;
	}

	public void setUnknownClass(Integer unknownClass) {
		this.unknownClass = unknownClass;
	}
}
