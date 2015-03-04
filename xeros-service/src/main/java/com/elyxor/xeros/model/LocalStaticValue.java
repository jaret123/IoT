package com.elyxor.xeros.model;

import javax.persistence.*;

@Entity
@Table(name = "xeros_local_static_values")
public class LocalStaticValue {

	public LocalStaticValue() {}
	
	private int id;
	private Integer classification;
    private Float coldWater;
    private Float hotWater;
    private Integer runTime;
    private Float manufacturerColdWater;
    private Float manufacturerHotWater;
    private Integer manufacturerRunTime;

    @Id
    @Column(name = "local_static_values_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "classification")
	public Integer getClassification() {
		return classification;
	}

	public void setClassification(Integer classification) {
		this.classification = classification;
	}

    @Column(name = "cold_water_gallons")
    public Float getColdWater() {
        return coldWater;
    }

    public void setColdWater(Float coldWater) {
        this.coldWater = coldWater;
    }

    @Column(name = "hot_water_gallons")
    public Float getHotWater() {
        return hotWater;
    }

    public void setHotWater(Float hotWater) {
        this.hotWater = hotWater;
    }

    @Column(name = "run_time")
    public Integer getRunTime() {return runTime;}
    public void setRunTime(Integer runTime) {this.runTime = runTime;}

    @Column(name = "manufacturer_cold_water")
    public Float getManufacturerColdWater() {
        return manufacturerColdWater;
    }

    public void setManufacturerColdWater(Float manufacturerColdWater) {
        this.manufacturerColdWater = manufacturerColdWater;
    }

    @Column(name = "manufacturer_hot_water")
    public Float getManufacturerHotWater() {
        return manufacturerHotWater;
    }

    public void setManufacturerHotWater(Float manufacturerHotWater) {
        this.manufacturerHotWater = manufacturerHotWater;
    }

    @Column(name = "manufacturer_run_time")
    public Integer getManufacturerRunTime() {
        return manufacturerRunTime;
    }

    public void setManufacturerRunTime(Integer manufacturerRunTime) {
        this.manufacturerRunTime = manufacturerRunTime;
    }
}
