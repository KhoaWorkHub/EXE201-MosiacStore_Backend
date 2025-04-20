package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.region.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

}
