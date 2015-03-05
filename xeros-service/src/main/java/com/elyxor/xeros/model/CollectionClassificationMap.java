package com.elyxor.xeros.model;

import javax.persistence.*;
import java.util.Collection;

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

	@ManyToOne(fetch = FetchType.LAZY)
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
