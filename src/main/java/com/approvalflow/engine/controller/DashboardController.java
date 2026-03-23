package com.approvalflow.engine.controller;

import com.approvalflow.engine.dto.response.DashboardSummaryResponse;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.repository.ApprovalRequestRepository;
import com.approvalflow.engine.repository.ApprovalStepRepository;
import com.approvalflow.engine.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private final ApprovalStepRepository stepRepository;
    private final ApprovalRequestRepository requestRepository;
    private final UserService userService;
    
    public DashboardController(ApprovalStepRepository stepRepository,
                              ApprovalRequestRepository requestRepository,
                              UserService userService) {
        this.stepRepository = stepRepository;
        this.requestRepository = requestRepository;
        this.userService = userService;
    }
    
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        
        long pendingMyApprovals = stepRepository.countPendingStepsByAssignee(user.getId());
        long overdueApprovals = stepRepository.countOverdueStepsByAssignee(user.getId(), LocalDate.now());
        
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        long approvedThisWeek = requestRepository.countByStatusAndUpdatedAtAfter(
                RequestStatus.APPROVED, weekAgo);
        long rejectedThisWeek = requestRepository.countByStatusAndUpdatedAtAfter(
                RequestStatus.REJECTED, weekAgo);
        
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .pendingMyApprovals(pendingMyApprovals)
                .overdueApprovals(overdueApprovals)
                .approvedThisWeek(approvedThisWeek)
                .rejectedThisWeek(rejectedThisWeek)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
