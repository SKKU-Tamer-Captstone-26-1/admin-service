package com.ontheblock.admin.service;

import com.ontheblock.admin.domain.audit.AuditLogRepository;
import com.ontheblock.admin.domain.audit.entity.AuditLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorId, String action, String targetType, UUID targetId, String detailJson) {
        auditLogRepository.save(AuditLogEntity.builder()
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detailJson(detailJson)
                .build());
    }
}
