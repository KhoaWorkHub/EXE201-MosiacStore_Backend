package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.address.AdministrativeRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministrativeRegionRepository extends JpaRepository<AdministrativeRegion, Integer> {
}