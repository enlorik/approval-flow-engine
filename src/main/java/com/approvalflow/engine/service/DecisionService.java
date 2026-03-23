package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.request.CreateDecisionDto;
import com.approvalflow.engine.dto.response.DecisionResponse;
import com.approvalflow.engine.entity.*;
import com.approvalflow.engine.enums.*;
import com.approvalflow.engine.exception.InvalidStateException;
import com.approvalflow.engine.exception.ResourceNotFoundException;
import com.approvalflow.engine.exception.UnauthorizedException;
import com.approvalflow.engine.repository.ApprovalDecisionRepository;
import com.approvalflow.engine.repository.ApprovalRequestRepository;
import com.approvalflow.engine.repository.ApprovalStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
public class DecisionService {
    
    private final ApprovalStepRepository stepRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final ApprovalRequestRepository requestRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    
    public DecisionService(ApprovalStepRepository stepRepository,
                          ApprovalDecisionRepository decisionRepository,
                          ApprovalRequestRepository requestRepository,
                          AuditLogService auditLogService,
                          NotificationService notificationService) {
        this.stepRepository = stepRepository;
        this.decisionRepository = decisionRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public DecisionResponse makeDecision(Long stepId, CreateDecisionDto dto, DecisionType decisionType, User decider) {
        ApprovalStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalStep", "id", stepId));
        
        if (step.getStatus() != StepStatus.IN_PROGRESS) {
            throw new InvalidStateException("Step is not in progress");
        }
        
        boolean isAssigned = step.getAssignments().stream()
                .anyMatch(assignment -> {
                    if (assignment.getAssignedUser() != null && 
                        assignment.getAssignedUser().getId().equals(decider.getId())) {
                        return true;
                    }
                    if (assignment.getAssignedRole() != null) {
                        return decider.getRoles().stream()
                                .anyMatch(role -> role.getId().equals(assignment.getAssignedRole().getId()));
                    }
                    return false;
                });
        
        if (!isAssigned) {
            throw new UnauthorizedException("You are not assigned to this approval step");
        }
        
        ApprovalDecision decision = ApprovalDecision.builder()
                .approvalStep(step)
                .decidedBy(decider)
                .decisionType(decisionType)
                .comment(dto.getComment())
                .decidedAt(LocalDateTime.now())
                .build();
        
        decision = decisionRepository.save(decision);
        
        ApprovalRequest request = step.getApprovalRequest();
        
        switch (decisionType) {
            case APPROVED:
                step.setStatus(StepStatus.APPROVED);
                step.setCompletedAt(LocalDateTime.now());
                stepRepository.save(step);
                
                boolean isLastStep = request.getSteps().stream()
                        .noneMatch(s -> s.getStepOrder() > step.getStepOrder());
                
                if (isLastStep) {
                    request.setStatus(RequestStatus.APPROVED);
                    requestRepository.save(request);
                    
                    auditLogService.logAction(decider, AuditAction.REQUEST_APPROVED, 
                            "ApprovalRequest", request.getId(), 
                            "Request approved: " + request.getTitle());
                    
                    notificationService.createNotification(
                            request.getRequester(),
                            "Request Approved",
                            "Your request '" + request.getTitle() + "' has been approved"
                    );
                } else {
                    ApprovalStep nextStep = request.getSteps().stream()
                            .filter(s -> s.getStepOrder() > step.getStepOrder())
                            .min(Comparator.comparing(ApprovalStep::getStepOrder))
                            .orElseThrow(() -> new InvalidStateException("Next step not found"));
                    
                    nextStep.setStatus(StepStatus.IN_PROGRESS);
                    stepRepository.save(nextStep);
                    request.setCurrentStepOrder(nextStep.getStepOrder());
                    requestRepository.save(request);
                    
                    nextStep.getAssignments().forEach(assignment -> {
                        if (assignment.getAssignedUser() != null) {
                            notificationService.createNotification(
                                    assignment.getAssignedUser(),
                                    "Approval Required",
                                    "Request '" + request.getTitle() + "' requires your approval"
                            );
                        }
                    });
                }
                
                auditLogService.logAction(decider, AuditAction.STEP_APPROVED, 
                        "ApprovalStep", step.getId(), 
                        "Step approved: " + step.getStepName());
                break;
                
            case REJECTED:
                step.setStatus(StepStatus.REJECTED);
                step.setCompletedAt(LocalDateTime.now());
                stepRepository.save(step);
                
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                
                auditLogService.logAction(decider, AuditAction.REQUEST_REJECTED, 
                        "ApprovalRequest", request.getId(), 
                        "Request rejected: " + request.getTitle());
                
                auditLogService.logAction(decider, AuditAction.STEP_REJECTED, 
                        "ApprovalStep", step.getId(), 
                        "Step rejected: " + step.getStepName());
                
                notificationService.createNotification(
                        request.getRequester(),
                        "Request Rejected",
                        "Your request '" + request.getTitle() + "' has been rejected"
                );
                break;
                
            case CHANGES_REQUESTED:
                step.setStatus(StepStatus.CHANGES_REQUESTED);
                stepRepository.save(step);
                
                request.setStatus(RequestStatus.CHANGES_REQUESTED);
                requestRepository.save(request);
                
                auditLogService.logAction(decider, AuditAction.REQUEST_CHANGES_REQUESTED, 
                        "ApprovalRequest", request.getId(), 
                        "Changes requested: " + request.getTitle());
                
                notificationService.createNotification(
                        request.getRequester(),
                        "Changes Requested",
                        "Changes have been requested for '" + request.getTitle() + "'"
                );
                break;
        }
        
        auditLogService.logAction(decider, AuditAction.DECISION_MADE, 
                "ApprovalDecision", decision.getId(), 
                "Decision made: " + decisionType + " on step " + step.getStepName());
        
        return DecisionResponse.builder()
                .id(decision.getId())
                .stepId(step.getId())
                .decidedByUsername(decider.getUsername())
                .decisionType(decision.getDecisionType())
                .comment(decision.getComment())
                .decidedAt(decision.getDecidedAt())
                .build();
    }
}
