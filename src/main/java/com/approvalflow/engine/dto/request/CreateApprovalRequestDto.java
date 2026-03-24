package com.approvalflow.engine.dto.request;

import com.approvalflow.engine.enums.RequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateApprovalRequestDto {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Request type is required")
    private RequestType requestType;
    
    private LocalDate dueDate;
    
    @NotEmpty(message = "At least one approval step is required")
    @Valid
    private List<CreateApprovalStepDto> steps;
}
