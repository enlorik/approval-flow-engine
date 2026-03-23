package com.approvalflow.engine.controller;

import com.approvalflow.engine.dto.response.ApprovalRequestResponse;
import com.approvalflow.engine.dto.response.ApprovalStepResponse;
import com.approvalflow.engine.dto.response.PagedResponse;
import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.enums.RequestType;
import com.approvalflow.engine.service.ApprovalRequestService;
import com.approvalflow.engine.service.EscalationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminController {
    
    private final EscalationService escalationService;
    private final ApprovalRequestService requestService;
    
    public AdminController(EscalationService escalationService,
                          ApprovalRequestService requestService) {
        this.escalationService = escalationService;
        this.requestService = requestService;
    }
    
    @GetMapping("/overdue-steps")
    public ResponseEntity<PagedResponse<ApprovalStepResponse>> getOverdueSteps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate"));
        PagedResponse<ApprovalStepResponse> response = escalationService.getOverdueSteps(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/requests")
    public ResponseEntity<PagedResponse<ApprovalRequestResponse>> getAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) RequestType requestType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        PagedResponse<ApprovalRequestResponse> response = requestService.getRequests(
                pageable, status, requesterId, null, requestType);
        
        return ResponseEntity.ok(response);
    }
}
