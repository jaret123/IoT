package com.elyxor.xeros.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "xeros_company")
public class Company {

	public Company() {
	}

	private int id;
    private String name;

	@Id
	@Column(name = "company_id", columnDefinition = "INT unsigned", updatable=false, insertable=false)
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

    @Column(name = "name")
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
