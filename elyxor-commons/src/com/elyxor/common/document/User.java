package com.elyxor.common.document;

import java.util.Date;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;

/**
 * @author Kemal Yurderi
 * 
 *         User DAO to access (bidirectional) the data in mongodb using Morphia
 * 
 */
@Entity
public class User {

	@Id
	private ObjectId id;

	private String firstName;
	private String lastName;
	@Indexed(unique = true, dropDups = true)
	private String twitterName;

	public User() {
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public void setId(String id) {
		this.id = new ObjectId(id);
	}
	
	public String getTwitterName() {
		return twitterName;
	}

	public void setTwitterName(String twitterName) {
		this.twitterName = twitterName;
	}

	public String getFirstName() {
		return firstName;
	}

  	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
