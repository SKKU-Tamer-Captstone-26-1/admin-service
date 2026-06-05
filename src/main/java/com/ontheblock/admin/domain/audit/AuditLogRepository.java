package com.ontheblock.admin.domain.audit;

import com.ontheblock.admin.domain.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
