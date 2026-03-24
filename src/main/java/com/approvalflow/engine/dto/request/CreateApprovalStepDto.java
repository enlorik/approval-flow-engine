package com.approvalflow.engine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalStepDto {
    
    @NotBlank(message = "Step name is required")
    private String stepName;
    
    @NotNull(message = "Step order is required")
    private Integer stepOrder;
    
    private LocalDate dueDate;
    
    private List<Long> assignedUserIds;
    
    private List<Long> assignedRoleIds;
}
