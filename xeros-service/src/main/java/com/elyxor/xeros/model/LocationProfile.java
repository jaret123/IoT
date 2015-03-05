package com.elyxor.xeros.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by will on 3/1/15.
 */

@Entity
@Table(name = "xeros_location_profile")
public class LocationProfile {
    public LocationProfile() {}

    private int locationId;
    private float costPerGallon;

    @Id
    @Column(name = "location_id")
    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    @Column(name = "cost_per_gallon")
    public float getCostPerGallon() {
        return costPerGallon;
    }

    public void setCostPerGallon(float costPerGallon) {
        this.costPerGallon = costPerGallon;
    }
}
