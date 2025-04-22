package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.region.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {

    Optional<Region> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE p.region.id = :regionId")
    boolean hasProducts(@Param("regionId") UUID regionId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TourGuide t WHERE t.region.id = :regionId")
    boolean hasTourGuides(@Param("regionId") UUID regionId);
}