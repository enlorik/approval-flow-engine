package com.approvalflow.engine.repository;

import com.approvalflow.engine.entity.ApproverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApproverAssignmentRepository extends JpaRepository<ApproverAssignment, Long> {
    
    List<ApproverAssignment> findByApprovalStepId(Long stepId);
}
