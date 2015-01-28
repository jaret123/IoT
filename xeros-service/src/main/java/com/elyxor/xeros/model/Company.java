package com.elyxor.xeros.model;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "xeros_company")
public class Company {

	public Company() {
	}

	private int id;
    private String name;
    private Collection<Location> locations;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "company")
    public Collection<Location> getLocations() {return locations;}

    public void setLocations(Collection<Location> locations) {this.locations = locations;}

}
