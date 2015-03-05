package com.elyxor.xeros.model;

import javax.persistence.*;

@Entity
@Table(name = "xeros_static_values")
public class StaticValue {

	public StaticValue() {}
	
	private String name;
	private String value;

    @Id
    @Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @Column(name = "value")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
