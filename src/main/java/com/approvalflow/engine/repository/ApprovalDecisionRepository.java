package com.approvalflow.engine.repository;

import com.approvalflow.engine.entity.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {
    
    List<ApprovalDecision> findByApprovalStepId(Long stepId);
}
