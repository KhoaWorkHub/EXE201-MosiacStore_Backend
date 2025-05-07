package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.address.AdministrativeUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, Integer> {
}