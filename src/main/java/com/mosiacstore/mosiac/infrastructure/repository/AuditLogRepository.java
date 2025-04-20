package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
