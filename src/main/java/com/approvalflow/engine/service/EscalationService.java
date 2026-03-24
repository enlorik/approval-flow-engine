package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.response.ApprovalStepResponse;
import com.approvalflow.engine.dto.response.DecisionResponse;
import com.approvalflow.engine.dto.response.PagedResponse;
import com.approvalflow.engine.entity.ApprovalStep;
import com.approvalflow.engine.enums.AuditAction;
import com.approvalflow.engine.repository.ApprovalStepRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EscalationService {
    
    private final ApprovalStepRepository stepRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    
    public EscalationService(ApprovalStepRepository stepRepository,
                           AuditLogService auditLogService,
                           NotificationService notificationService) {
        this.stepRepository = stepRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }
    
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void checkOverdueSteps() {
        LocalDate today = LocalDate.now();
        List<ApprovalStep> overdueSteps = stepRepository.findOverdueSteps(today);
        
        for (ApprovalStep step : overdueSteps) {
            auditLogService.logAction(null, AuditAction.STEP_ESCALATED, 
                    "ApprovalStep", step.getId(), 
                    "Step overdue: " + step.getStepName() + " (Due: " + step.getDueDate() + ")");
            
            step.getAssignments().forEach(assignment -> {
                if (assignment.getAssignedUser() != null) {
                    notificationService.createNotification(
                            assignment.getAssignedUser(),
                            "Overdue Approval",
                            "Approval step '" + step.getStepName() + "' is overdue"
                    );
                }
            });
        }
    }
    
    @Transactional(readOnly = true)
    public PagedResponse<ApprovalStepResponse> getOverdueSteps(Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<ApprovalStep> page = stepRepository.findOverdueSteps(today, pageable);
        
        List<ApprovalStepResponse> content = page.getContent().stream()
                .map(this::mapToStepResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<ApprovalStepResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    
    private ApprovalStepResponse mapToStepResponse(ApprovalStep step) {
        List<ApprovalStepResponse.AssignmentInfo> assignments = step.getAssignments().stream()
                .map(a -> ApprovalStepResponse.AssignmentInfo.builder()
                        .assignmentId(a.getId())
                        .assignedUsername(a.getAssignedUser() != null ? a.getAssignedUser().getUsername() : null)
                        .assignedRoleName(a.getAssignedRole() != null ? a.getAssignedRole().getName().name() : null)
                        .build())
                .collect(Collectors.toList());
        
        List<DecisionResponse> decisions = step.getDecisions().stream()
                .map(d -> DecisionResponse.builder()
                        .id(d.getId())
                        .stepId(step.getId())
                        .decidedByUsername(d.getDecidedBy().getUsername())
                        .decisionType(d.getDecisionType())
                        .comment(d.getComment())
                        .decidedAt(d.getDecidedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ApprovalStepResponse.builder()
                .id(step.getId())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .status(step.getStatus())
                .dueDate(step.getDueDate())
                .completedAt(step.getCompletedAt())
                .assignments(assignments)
                .decisions(decisions)
                .build();
    }
}
