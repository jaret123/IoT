package com.elyxor.xeros.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "xeros_cycle")
public class Cycle {

	public Cycle() {}

    private int id;
    private Timestamp readingTimestamp;
    private Float coldWaterVolume;
    private Float hotWaterVolume;
    private Float therms;
    private Float runTime;

    private Location location;
    private Machine machine;
    private Classification classification;
    private DaiMeterActual daiMeterActual;
    private DaiMeterCollection daiMeterCollection;

    @Id
    @Column(name = "dai_meter_actual_id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "reading_timestamp")
	public Timestamp getReadingTimestamp() {
		return readingTimestamp;
	}

	public void setReadingTimestamp(Timestamp readingTimestamp) {
		this.readingTimestamp = readingTimestamp;
	}
	
	@Column(name = "cycle_cold_water_volume")
	public Float getColdWaterVolume() {
		return coldWaterVolume;
	}

	public void setColdWaterVolume(Float coldWaterVolume) {
		this.coldWaterVolume = coldWaterVolume;
	}

    @Column(name = "cycle_hot_water_volume")
    public Float getHotWaterVolume() {return hotWaterVolume;}

    public void setHotWaterVolume(Float hotWaterVolume) {this.hotWaterVolume = hotWaterVolume;}

	@Column(name = "cycle_therms")
	public Float getTherms() {
		return therms;
	}

	public void setTherms(Float therms) {
		this.therms = therms;
	}

	@Column(name = "cycle_time_run_time")
	public Float getRunTime() {
		return runTime;
	}

	public void setRunTime(Float runTime) {
		this.runTime = runTime;
	}

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dai_meter_actual_id", referencedColumnName = "dai_meter_actual_id")
    public DaiMeterActual getDaiMeterActual() {
        return daiMeterActual;
    }

    public void setDaiMeterActual(DaiMeterActual daiMeterActual) {
        this.daiMeterActual = daiMeterActual;
    }

    @ManyToOne()
    @JoinColumn(name = "location_id", referencedColumnName = "location_id")
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @ManyToOne()
    @JoinColumn(name = "machine_id", referencedColumnName = "machine_id")
    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    @ManyToOne()
    @JoinColumn(name = "classification_id", referencedColumnName = "classification_id")
    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }
}
