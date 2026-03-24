package com.approvalflow.engine.entity;

import com.approvalflow.engine.enums.DecisionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_step_id", nullable = false)
    private ApprovalStep approvalStep;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 50)
    private DecisionType decisionType;
    
    @Column(length = 2000)
    private String comment;
    
    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
