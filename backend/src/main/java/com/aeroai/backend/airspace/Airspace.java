package com.aeroai.backend.airspace;

import jakarta.persistence.*;

@Entity
@Table(name = "airspaces")
public class Airspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "default_zoom", nullable = false)
    private int defaultZoom;

    @Column(name = "lamin", nullable = false)
    private double lamin;

    @Column(name = "lomin", nullable = false)
    private double lomin;

    @Column(name = "lamax", nullable = false)
    private double lamax;

    @Column(name = "lomax", nullable = false)
    private double lomax;

    @Column(name = "display_region")
    private String displayRegion;

    @Column(name = "aliases")
    private String aliases;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    protected Airspace() {
    }

    public Long getId() { return id; }
    public String getCityName() { return cityName; }
    public String getCountry() { return country; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getDefaultZoom() { return defaultZoom; }
    public double getLamin() { return lamin; }
    public double getLomin() { return lomin; }
    public double getLamax() { return lamax; }
    public double getLomax() { return lomax; }
    public String getDisplayRegion() { return displayRegion; }
    public String getAliases() { return aliases; }
    public String getSlug() { return slug; }
}
