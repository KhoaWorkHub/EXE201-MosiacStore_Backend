package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.address.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
}