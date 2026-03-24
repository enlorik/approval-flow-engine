package com.approvalflow.engine.dto.response;

import com.approvalflow.engine.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private Long id;
    private String performedByUsername;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String details;
    private LocalDateTime createdAt;
}
