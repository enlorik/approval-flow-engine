package com.approvalflow.engine.service;

import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.AuditAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.approvalflow.engine.entity.AuditLog;
import com.approvalflow.engine.repository.AuditLogRepository;

@Service
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    @Transactional
    public void logAction(User user, AuditAction action, String entityType, Long entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .performedBy(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        
        auditLogRepository.save(auditLog);
    }
}
