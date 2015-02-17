package com.elyxor.xeros.model;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "xeros_location")
public class Location {

	public Location() {
	}

	private int id;
    private Company company;
    private String name;
    private Collection<Machine> machines;

	@Id
	@Column(name = "location_id", columnDefinition = "INT unsigned", updatable=false, insertable=false)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", referencedColumnName = "company_id")
    public Company getCompany() {return company;}
    public void setCompany(Company company) {this.company = company;}

    @Column(name = "location_name")
    public String getName() {return this.name;}
    public void setName(String name) {this.name = name;}

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "location")
    public Collection<Machine> getMachines() {return machines;}

    public void setMachines(Collection<Machine> machines) {this.machines = machines;}
}
