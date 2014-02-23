package com.elyxor.xeros.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_location")
public class Location {

	public Location() {
	}

	private int id;

	@Id
	@Column(name = "location_id", columnDefinition = "INT unsigned")
	@GeneratedValue(strategy = GenerationType.AUTO)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
