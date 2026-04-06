package com.workreport.service;

import com.workreport.annotation.PublicApi;
import com.workreport.entity.AuditLog;
import com.workreport.enums.AuditActionType;
import com.workreport.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @PublicApi
    @Transactional
    public void log(AuditActionType action, Long actorId, String targetEntity, Long targetId, String summary) {
        AuditLog auditLog = AuditLog.builder()
                .actionType(action)
                .actorId(actorId)
                .targetEntity(targetEntity)
                .targetId(targetId)
                .summary(summary)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
