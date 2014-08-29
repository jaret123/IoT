package com.elyxor.xeros.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "xeros_status")
public class MachineStatus {

	public MachineStatus() {}

	private Integer id;
    private Integer machineId;
	private String daiIdentifier;
	private Timestamp timestamp;
    private Integer statusCode;
    private String statusMessage;

    @Id
    @Column(name = "status_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "machine_id")
    public Integer getMachineId() {
        return machineId;
    }

    public void setMachineId(Integer machineId) {
        this.machineId = machineId;
    }

    @Column(name = "dai_identifier")
    public String getDaiIdentifier() {
        return daiIdentifier;
    }

    public void setDaiIdentifier(String daiIdentifier) {
        this.daiIdentifier = daiIdentifier;
    }

    @Column(name = "timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Column(name = "status_code")
    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Column(name = "status_message")
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
