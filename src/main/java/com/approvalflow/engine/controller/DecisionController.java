package com.approvalflow.engine.controller;

import com.approvalflow.engine.dto.request.CreateDecisionDto;
import com.approvalflow.engine.dto.response.DecisionResponse;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.DecisionType;
import com.approvalflow.engine.service.DecisionService;
import com.approvalflow.engine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/steps")
public class DecisionController {
    
    private final DecisionService decisionService;
    private final UserService userService;
    
    public DecisionController(DecisionService decisionService,
                             UserService userService) {
        this.decisionService = decisionService;
        this.userService = userService;
    }
    
    @PostMapping("/{stepId}/approve")
    public ResponseEntity<DecisionResponse> approveStep(
            @PathVariable Long stepId,
            @Valid @RequestBody CreateDecisionDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        DecisionResponse response = decisionService.makeDecision(stepId, dto, DecisionType.APPROVED, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{stepId}/reject")
    public ResponseEntity<DecisionResponse> rejectStep(
            @PathVariable Long stepId,
            @Valid @RequestBody CreateDecisionDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        DecisionResponse response = decisionService.makeDecision(stepId, dto, DecisionType.REJECTED, user);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{stepId}/request-changes")
    public ResponseEntity<DecisionResponse> requestChanges(
            @PathVariable Long stepId,
            @Valid @RequestBody CreateDecisionDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        DecisionResponse response = decisionService.makeDecision(stepId, dto, DecisionType.CHANGES_REQUESTED, user);
        return ResponseEntity.ok(response);
    }
}
