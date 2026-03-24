package com.approvalflow.engine.entity;

import com.approvalflow.engine.enums.StepStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "step_name", nullable = false, length = 255)
    private String stepName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "approvalStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApproverAssignment> assignments = new ArrayList<>();
    
    @OneToMany(mappedBy = "approvalStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decidedAt DESC")
    @Builder.Default
    private List<ApprovalDecision> decisions = new ArrayList<>();
}
