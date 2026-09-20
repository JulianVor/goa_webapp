package de.goafestival.webapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Backing bean for the admin "create/edit location" form. */
public class LocationForm {

    private Long id;

    @NotBlank
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
}
