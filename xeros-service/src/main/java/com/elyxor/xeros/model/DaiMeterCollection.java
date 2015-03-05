package com.elyxor.xeros.model;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;

@Entity
@Table(name = "xeros_dai_meter_collection")
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

    private Machine machine;
    private CollectionClassificationMap collectionClassificationMap;
    private Collection<DaiMeterCollectionDetail> collectionDetails;
    private DaiMeterActual daiMeterActual;

    private float earliestValue;
    private Integer exception;

    @Id
    @Column(columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "machine_identifier", length=255)
	public String getMachineIdentifier() {
		return machineIdentifier;
	}

	public void setMachineIdentifier(String machineIdentifier) {
		this.machineIdentifier = machineIdentifier;
	}

	@Column(name = "dai_write_timestamp")
	public Timestamp getDaiCollectionTime() {
		return daiCollectionTime;
	}

	public void setDaiCollectionTime(Timestamp daiCollectionTime) {
		this.daiCollectionTime = daiCollectionTime;
	}

	@Column(name="file_upload_timestamp")
	public Timestamp getFileUploadTime() {
		return fileUploadTime;
	}

	public void setFileUploadTime(Timestamp fileUploadTime) {
		this.fileUploadTime = fileUploadTime;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", referencedColumnName = "machine_id")
	public Machine getMachine() {
		return machine;
	}

	public void setMachine(Machine machine) {
		this.machine = machine;
	}

	@Column(name = "olson_timezone_id", length=64)
	public String getOlsonTimezoneId() {
		return olsonTimezoneId;
	}

	public void setOlsonTimezoneId(String olsonTimezoneId) {
		this.olsonTimezoneId = olsonTimezoneId;
	}

	@Column(name = "file_create_timestamp")
	public Timestamp getFileCreateTime() {
		return fileCreateTime;
	}

	public void setFileCreateTime(Timestamp fileCreateTime) {
		this.fileCreateTime = fileCreateTime;
	}

	@Column(name = "location_identifier", length=64)
	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	@Column(name = "dai_identifier", length=64)
	public String getDaiIdentifier() {
		return daiIdentifier;
	}

	public void setDaiIdentifier(String daiIdentifier) {
		this.daiIdentifier = daiIdentifier;
	}

	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dai_meter_actual_id", referencedColumnName = "dai_meter_actual_id")
	public DaiMeterActual getDaiMeterActual() {
		return daiMeterActual;
	}

	public void setDaiMeterActual(DaiMeterActual daiMeterActual) {
		this.daiMeterActual = daiMeterActual;
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
    @Column(name = "earliestValue")
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
