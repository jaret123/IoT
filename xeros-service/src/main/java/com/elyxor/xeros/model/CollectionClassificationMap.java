package com.elyxor.xeros.model;

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
@Table(name = "xeros_collection_map")
public class CollectionClassificationMap {

	public CollectionClassificationMap() {}
	
	private int id;
	private Machine machine;
	private Classification classification;
	private Collection<CollectionClassificationMapDetail> collectionDetails;
	
    @Id
    @Column(name = "collection_map_id", columnDefinition = "INT unsigned")
    @GeneratedValue(strategy=GenerationType.AUTO)    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", referencedColumnName = "machine_id")
	public Machine getMachine() {
		return machine;
	}

	public void setMachine(Machine machine) {
		this.machine = machine;
	}

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classification_id", referencedColumnName = "classification_id")
	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "collectionClassificationMap")
	public Collection<CollectionClassificationMapDetail> getCollectionDetails() {
		return collectionDetails;
	}

	public void setCollectionDetails(Collection<CollectionClassificationMapDetail> collectionDetails) {
		this.collectionDetails = collectionDetails;
	}

	

	
}
