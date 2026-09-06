package com.aeroai.backend.airport;

import jakarta.persistence.*;

@Entity
@Table(name = "airports")
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "iata")
    private String iata;

    @Column(name = "icao")
    private String icao;

    @Column(name = "city")
    private String city;

    @Column(name = "location")
    private String location;

    @Column(name = "country")
    private String country;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    protected Airport() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getIata() { return iata; }
    public String getIcao() { return icao; }
    public String getCity() { return city; }
    public String getLocation() { return location; }
    public String getCountry() { return country; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
