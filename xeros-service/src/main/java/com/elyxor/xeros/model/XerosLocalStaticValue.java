package com.elyxor.xeros.model;

import javax.persistence.*;

@Entity
@Table(name = "xeros_xeros_local_static_value")
public class XerosLocalStaticValue {

	public XerosLocalStaticValue() {}
	
	private int id;
	private Classification classification;
    private Float coldWater;
    private Float hotWater;
    private Integer runTime;

    @Id
    @Column(name = "xeros_local_static_value_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", referencedColumnName = "classification_id")
	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
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
}
