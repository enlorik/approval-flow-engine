package com.approvalflow.engine.controller;

import com.approvalflow.engine.dto.request.CreateApprovalRequestDto;
import com.approvalflow.engine.dto.response.ApprovalRequestDetailResponse;
import com.approvalflow.engine.dto.response.ApprovalRequestResponse;
import com.approvalflow.engine.dto.response.AuditLogResponse;
import com.approvalflow.engine.dto.response.PagedResponse;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.enums.RequestType;
import com.approvalflow.engine.service.ApprovalRequestService;
import com.approvalflow.engine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class ApprovalRequestController {
    
    private final ApprovalRequestService requestService;
    private final UserService userService;
    
    public ApprovalRequestController(ApprovalRequestService requestService,
                                    UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }
    
    @PostMapping
    public ResponseEntity<ApprovalRequestResponse> createRequest(
            @Valid @RequestBody CreateApprovalRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        ApprovalRequestResponse response = requestService.createRequest(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<ApprovalRequestResponse>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) RequestType requestType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        PagedResponse<ApprovalRequestResponse> response = requestService.getRequests(
                pageable, status, requesterId, approverId, requestType);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestDetailResponse> getRequestDetail(@PathVariable Long id) {
        ApprovalRequestDetailResponse response = requestService.getRequestDetail(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApprovalRequestResponse> submitRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        ApprovalRequestResponse response = requestService.submitRequest(id, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        requestService.cancelRequest(id, user);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApprovalRequestDetailResponse> getRequestTimeline(@PathVariable Long id) {
        ApprovalRequestDetailResponse response = requestService.getRequestDetail(id);
        return ResponseEntity.ok(response);
    }
}
