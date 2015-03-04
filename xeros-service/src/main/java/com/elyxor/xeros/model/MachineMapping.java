package com.elyxor.xeros.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_machine_pairing")
public class MachineMapping {

	public MachineMapping() {}
	
	private Integer daiId;
	private Integer ekId;

    @Id
    @Column(name = "dai_machine_id", columnDefinition = "INT unsigned")
	public Integer getDaiId() {
		return daiId;
	}

	public void setDaiId(Integer daiId) {
		this.daiId = daiId;
	}

	@Column(name = "ek_machine_id", columnDefinition = "INT unsigned")
	public Integer getEkId() {
		return ekId;
	}

	public void setEkId(Integer ekId) {
		this.ekId = ekId;
	}
}
