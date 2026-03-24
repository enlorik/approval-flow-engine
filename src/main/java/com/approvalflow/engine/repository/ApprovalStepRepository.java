package com.approvalflow.engine.repository;

import com.approvalflow.engine.entity.ApprovalStep;
import com.approvalflow.engine.enums.StepStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    
    List<ApprovalStep> findByApprovalRequestId(Long requestId);
    
    @Query("SELECT s FROM ApprovalStep s WHERE s.status = 'IN_PROGRESS' AND s.dueDate < :today")
    List<ApprovalStep> findOverdueSteps(@Param("today") LocalDate today);
    
    @Query("SELECT s FROM ApprovalStep s WHERE s.status = 'IN_PROGRESS' AND s.dueDate < :today")
    Page<ApprovalStep> findOverdueSteps(@Param("today") LocalDate today, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM ApprovalStep s " +
           "JOIN s.assignments a " +
           "WHERE (a.assignedUser.id = :userId OR a.assignedRole.id IN " +
           "(SELECT r.id FROM User u JOIN u.roles r WHERE u.id = :userId)) " +
           "AND s.status IN ('PENDING', 'IN_PROGRESS')")
    long countPendingStepsByAssignee(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(s) FROM ApprovalStep s " +
           "JOIN s.assignments a " +
           "WHERE (a.assignedUser.id = :userId OR a.assignedRole.id IN " +
           "(SELECT r.id FROM User u JOIN u.roles r WHERE u.id = :userId)) " +
           "AND s.status = 'IN_PROGRESS' AND s.dueDate < :today")
    long countOverdueStepsByAssignee(@Param("userId") Long userId, @Param("today") LocalDate today);
}
