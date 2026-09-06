package com.aeroai.backend.airport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByIataIgnoreCase(String iata);

    Optional<Airport> findByIcaoIgnoreCase(String icao);

    @Query("""
            SELECT a FROM Airport a
            WHERE lower(a.name) LIKE lower(concat('%', :query, '%'))
               OR lower(coalesce(a.iata, '')) LIKE lower(concat(:query, '%'))
               OR lower(coalesce(a.icao, '')) LIKE lower(concat(:query, '%'))
               OR lower(coalesce(a.city, '')) LIKE lower(concat('%', :query, '%'))
            ORDER BY a.name ASC
            """)
    List<Airport> search(@Param("query") String query);

    @Query("SELECT a FROM Airport a WHERE lower(a.city) = lower(:city)")
    List<Airport> findByCityIgnoreCase(@Param("city") String city);

    @Query("""
            SELECT a FROM Airport a
            WHERE a.latitude BETWEEN :latMin AND :latMax
              AND a.longitude BETWEEN :lonMin AND :lonMax
            """)
    List<Airport> findWithinBoundingBox(@Param("latMin") double latMin, @Param("latMax") double latMax,
                                         @Param("lonMin") double lonMin, @Param("lonMax") double lonMax);
}
