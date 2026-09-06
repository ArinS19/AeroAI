package com.aeroai.backend.airspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AirspaceRepository extends JpaRepository<Airspace, Long> {

    Optional<Airspace> findBySlug(String slug);

    @Query("""
            SELECT a FROM Airspace a
            WHERE lower(a.cityName) LIKE lower(concat('%', :query, '%'))
               OR lower(a.country) LIKE lower(concat('%', :query, '%'))
               OR lower(coalesce(a.aliases, '')) LIKE lower(concat('%', :query, '%'))
            ORDER BY a.cityName ASC
            """)
    List<Airspace> search(@Param("query") String query);
}
