package com.elyxor.xeros.model;

import java.sql.Timestamp;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_dai_meter_collection")
public class DaiMeterCollection {
	
	public DaiMeterCollection() {}

    private int id;
    private String machineType;
    private String machineName;
    private Timestamp collectionTime;
    private Machine machine;
    private CollectionClassificationMap collectionClassificationMap;
    private Collection<DaiMeterCollectionDetail> collectionDetails;

    @Id
    @Column(columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)
	public int getId() {
		return id;
	}

	public void setId(int id) {
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
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", referencedColumnName = "machine_id")
	public Machine getMachine() {
		return machine;
	}

	public void setMachine(Machine machine) {
		this.machine = machine;
	}

	@Column(name = "machine_type")
	public String getMachineType() {
		return machineType;
	}

	public void setMachineType(String machineType) {
		this.machineType = machineType;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_map_id", referencedColumnName = "collection_map_id")
	public CollectionClassificationMap getCollectionClassificationMap() {
		return collectionClassificationMap;
	}

	public void setCollectionClassificationMap(
			CollectionClassificationMap collectionClassificationMap) {
		this.collectionClassificationMap = collectionClassificationMap;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "daiMeterCollection")
	public Collection<DaiMeterCollectionDetail> getCollectionDetails() {
		return collectionDetails;
	}

	public void setCollectionDetails(Collection<DaiMeterCollectionDetail> collectionDetails) {
		this.collectionDetails = collectionDetails;
	}

	@Override
	public String toString() {
		return String.format("DaiMeterCollection [id=%s, machineType=%s, machineName=%s, collectionTime=%s]", 
				id, machineType, machineName, collectionTime);
	}
}
