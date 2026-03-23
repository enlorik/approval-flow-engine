package com.approvalflow.engine.dto.response;

import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDetailResponse {
    
    private Long id;
    private String title;
    private String description;
    private RequestType requestType;
    private RequestStatus status;
    private String requesterUsername;
    private Integer currentStepOrder;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApprovalStepResponse> steps;
    private List<CommentResponse> comments;
    private List<AuditLogResponse> auditLogs;
}
