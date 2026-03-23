package com.approvalflow.engine.dto.response;

import com.approvalflow.engine.enums.StepStatus;
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
public class ApprovalStepResponse {
    
    private Long id;
    private Integer stepOrder;
    private String stepName;
    private StepStatus status;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private List<AssignmentInfo> assignments;
    private List<DecisionResponse> decisions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentInfo {
        private Long assignmentId;
        private String assignedUsername;
        private String assignedRoleName;
    }
}
