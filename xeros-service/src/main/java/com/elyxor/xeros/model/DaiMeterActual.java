package com.elyxor.xeros.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "xeros_dai_meter_actual")
public class DaiMeterActual {

	public DaiMeterActual() {}
	
	private int id;
	private Timestamp timestamp;
	private ActiveDai activeDai;
	private Classification classification;
	private Float hotWater;
	private Float coldWater;
	private Integer runTime;
	private Machine machine;
    private String exception;
	
	
    @Id
    @Column(name = "dai_meter_actual_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "reading_timestamp")	
	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_dai_id", referencedColumnName = "active_dai_id")	
	public ActiveDai getActiveDai() {
		return activeDai;
	}

	public void setActiveDai(ActiveDai activeDai) {
		this.activeDai = activeDai;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", referencedColumnName = "classification_id")	
	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	@Column(name = "hot_water", scale=10, precision=2)
	public Float getHotWater() {
		return hotWater;
	}

	public void setHotWater(float hotWater) {
		this.hotWater = hotWater;
	}

	@Column(name = "cold_water", scale=10, precision=2)
	public Float getColdWater() {
		return coldWater;
	}

	public void setColdWater(float coldWater) {
		this.coldWater = coldWater;
	}

	@Column(name = "run_time", scale=10)	
	public Integer getRunTime() {
		return runTime;
	}

	public void setRunTime(int runTime) {
		this.runTime = runTime;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", referencedColumnName = "machine_id")
	public Machine getMachine() {
		return machine;
	}

	public void setMachine(Machine machine) {
		this.machine = machine;
	}

    @Column(name = "exception")
    public String getException() { return this.exception;}
    public void setException(String exception) {this.exception = exception;}

	

	
}
