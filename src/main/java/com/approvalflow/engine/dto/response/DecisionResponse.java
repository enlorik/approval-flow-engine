package com.approvalflow.engine.dto.response;

import com.approvalflow.engine.enums.DecisionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResponse {
    
    private Long id;
    private Long stepId;
    private String decidedByUsername;
    private DecisionType decisionType;
    private String comment;
    private LocalDateTime decidedAt;
}
