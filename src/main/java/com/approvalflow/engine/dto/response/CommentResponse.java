package com.approvalflow.engine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private Long id;
    private Long requestId;
    private String authorUsername;
    private String content;
    private LocalDateTime createdAt;
}
