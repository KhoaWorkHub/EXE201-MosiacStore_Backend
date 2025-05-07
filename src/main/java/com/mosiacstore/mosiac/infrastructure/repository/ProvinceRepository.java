package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.address.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, String> {
}