package de.goafestival.webapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A venue, managed centrally so the same one can be reused across several
 * Editions (the yearly festival and any number of Kneipenkonzerte) instead
 * of retyping its address every time.
 */
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String street;
    private String zipCity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipCity() {
        return zipCity;
    }

    public void setZipCity(String zipCity) {
        this.zipCity = zipCity;
    }

    /** Address for the Google Maps embed + "Route berechnen" link, composed from street and zip/city. */
    public String getMapQuery() {
        boolean hasStreet = street != null && !street.isBlank();
        boolean hasZipCity = zipCity != null && !zipCity.isBlank();
        if (hasStreet && hasZipCity) {
            return street + ", " + zipCity;
        }
        if (hasStreet) {
            return street;
        }
        if (hasZipCity) {
            return zipCity;
        }
        return null;
    }
}
