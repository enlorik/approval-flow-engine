package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.request.CreateApprovalRequestDto;
import com.approvalflow.engine.dto.request.CreateApprovalStepDto;
import com.approvalflow.engine.dto.response.*;
import com.approvalflow.engine.entity.*;
import com.approvalflow.engine.enums.*;
import com.approvalflow.engine.exception.InvalidStateException;
import com.approvalflow.engine.exception.ResourceNotFoundException;
import com.approvalflow.engine.exception.UnauthorizedException;
import com.approvalflow.engine.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalRequestService {
    
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    
    public ApprovalRequestService(ApprovalRequestRepository requestRepository,
                                 ApprovalStepRepository stepRepository,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 AuditLogRepository auditLogRepository,
                                 AuditLogService auditLogService) {
        this.requestRepository = requestRepository;
        this.stepRepository = stepRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public ApprovalRequestResponse createRequest(CreateApprovalRequestDto dto, User requester) {
        ApprovalRequest request = ApprovalRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .requestType(dto.getRequestType())
                .status(RequestStatus.DRAFT)
                .requester(requester)
                .currentStepOrder(0)
                .dueDate(dto.getDueDate())
                .steps(new ArrayList<>())
                .build();
        
        request = requestRepository.save(request);
        
        for (CreateApprovalStepDto stepDto : dto.getSteps()) {
            ApprovalStep step = ApprovalStep.builder()
                    .approvalRequest(request)
                    .stepOrder(stepDto.getStepOrder())
                    .stepName(stepDto.getStepName())
                    .status(StepStatus.PENDING)
                    .dueDate(stepDto.getDueDate())
                    .assignments(new ArrayList<>())
                    .build();
            
            step = stepRepository.save(step);
            
            if (stepDto.getAssignedUserIds() != null) {
                for (Long userId : stepDto.getAssignedUserIds()) {
                    User assignedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    
                    ApproverAssignment assignment = ApproverAssignment.builder()
                            .approvalStep(step)
                            .assignedUser(assignedUser)
                            .build();
                    step.getAssignments().add(assignment);
                }
            }
            
            if (stepDto.getAssignedRoleIds() != null) {
                for (Long roleId : stepDto.getAssignedRoleIds()) {
                    Role assignedRole = roleRepository.findById(roleId)
                            .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
                    
                    ApproverAssignment assignment = ApproverAssignment.builder()
                            .approvalStep(step)
                            .assignedRole(assignedRole)
                            .build();
                    step.getAssignments().add(assignment);
                }
            }
            
            request.getSteps().add(step);
        }
        
        request = requestRepository.save(request);
        
        auditLogService.logAction(requester, AuditAction.REQUEST_CREATED, 
                "ApprovalRequest", request.getId(), 
                "Created request: " + request.getTitle());
        
        return mapToResponse(request);
    }
    
    @Transactional
    public ApprovalRequestResponse submitRequest(Long requestId, User user) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));
        
        if (!request.getRequester().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the requester can submit this request");
        }
        
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new InvalidStateException("Only DRAFT requests can be submitted");
        }
        
        if (request.getSteps().isEmpty()) {
            throw new InvalidStateException("Cannot submit request without approval steps");
        }
        
        // Transition: DRAFT → SUBMITTED → IN_REVIEW (activate first step in one save)
        ApprovalStep firstStep = request.getSteps().stream()
                .min(Comparator.comparing(ApprovalStep::getStepOrder))
                .orElseThrow(() -> new InvalidStateException("No approval steps found"));
        
        firstStep.setStatus(StepStatus.IN_PROGRESS);
        request.setCurrentStepOrder(firstStep.getStepOrder());
        request.setStatus(RequestStatus.IN_REVIEW);
        
        requestRepository.save(request);
        
        auditLogService.logAction(user, AuditAction.REQUEST_SUBMITTED, 
                "ApprovalRequest", request.getId(), 
                "Submitted request: " + request.getTitle());
        
        return mapToResponse(request);
    }
    
    @Transactional
    public void cancelRequest(Long requestId, User user) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));
        
        boolean isRequester = request.getRequester().getId().equals(user.getId());
        boolean isAdminOrManager = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN || 
                                 role.getName() == RoleName.ROLE_MANAGER);
        
        if (!isRequester && !isAdminOrManager) {
            throw new UnauthorizedException("Only the requester or admin/manager can cancel this request");
        }
        
        if (request.getStatus() == RequestStatus.APPROVED || 
            request.getStatus() == RequestStatus.REJECTED ||
            request.getStatus() == RequestStatus.CANCELLED) {
            throw new InvalidStateException("Cannot cancel a request that is already " + request.getStatus());
        }
        
        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
        
        auditLogService.logAction(user, AuditAction.REQUEST_CANCELLED, 
                "ApprovalRequest", request.getId(), 
                "Cancelled request: " + request.getTitle());
    }
    
    @Transactional(readOnly = true)
    public PagedResponse<ApprovalRequestResponse> getRequests(
            Pageable pageable,
            RequestStatus status,
            Long requesterId,
            Long approverId,
            RequestType requestType) {
        
        Page<ApprovalRequest> page;
        
        if (approverId != null) {
            page = requestRepository.findRequestsAssignedToUser(approverId, pageable);
        } else if (status != null || requesterId != null || requestType != null) {
            page = requestRepository.findByFilters(status, requesterId, requestType, pageable);
        } else {
            page = requestRepository.findAll(pageable);
        }
        
        List<ApprovalRequestResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<ApprovalRequestResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    
    @Transactional(readOnly = true)
    public ApprovalRequestDetailResponse getRequestDetail(Long id) {
        ApprovalRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", id));
        
        List<ApprovalStepResponse> steps = request.getSteps().stream()
                .map(this::mapToStepResponse)
                .collect(Collectors.toList());
        
        List<CommentResponse> comments = request.getComments().stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
        
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                "ApprovalRequest", id, Pageable.ofSize(50));
        List<AuditLogResponse> auditLogResponses = auditLogs.getContent().stream()
                .map(this::mapToAuditLogResponse)
                .collect(Collectors.toList());
        
        return ApprovalRequestDetailResponse.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .requesterUsername(request.getRequester().getUsername())
                .currentStepOrder(request.getCurrentStepOrder())
                .dueDate(request.getDueDate())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .steps(steps)
                .comments(comments)
                .auditLogs(auditLogResponses)
                .build();
    }
    
    private ApprovalRequestResponse mapToResponse(ApprovalRequest request) {
        return ApprovalRequestResponse.builder()
                .id(request.getId())
                .title(request.getTitle())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .requesterUsername(request.getRequester().getUsername())
                .currentStepOrder(request.getCurrentStepOrder())
                .dueDate(request.getDueDate())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
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
                .map(this::mapToDecisionResponse)
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
    
    private DecisionResponse mapToDecisionResponse(ApprovalDecision decision) {
        return DecisionResponse.builder()
                .id(decision.getId())
                .stepId(decision.getApprovalStep().getId())
                .decidedByUsername(decision.getDecidedBy().getUsername())
                .decisionType(decision.getDecisionType())
                .comment(decision.getComment())
                .decidedAt(decision.getDecidedAt())
                .build();
    }
    
    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .requestId(comment.getApprovalRequest().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
    
    private AuditLogResponse mapToAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .performedByUsername(log.getPerformedBy() != null ? log.getPerformedBy().getUsername() : "System")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
