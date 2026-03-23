package com.approvalflow.engine.repository;

import com.approvalflow.engine.entity.ApprovalRequest;
import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.enums.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    
    Page<ApprovalRequest> findByStatus(RequestStatus status, Pageable pageable);
    
    Page<ApprovalRequest> findByRequesterId(Long requesterId, Pageable pageable);
    
    Page<ApprovalRequest> findByRequestType(RequestType requestType, Pageable pageable);
    
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
           "JOIN ar.steps s " +
           "JOIN s.assignments a " +
           "WHERE (a.assignedUser.id = :userId OR a.assignedRole.id IN " +
           "(SELECT r.id FROM User u JOIN u.roles r WHERE u.id = :userId)) " +
           "AND s.status IN ('PENDING', 'IN_PROGRESS')")
    Page<ApprovalRequest> findRequestsAssignedToUser(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT ar FROM ApprovalRequest ar " +
           "WHERE (:status IS NULL OR ar.status = :status) " +
           "AND (:requesterId IS NULL OR ar.requester.id = :requesterId) " +
           "AND (:requestType IS NULL OR ar.requestType = :requestType)")
    Page<ApprovalRequest> findByFilters(
        @Param("status") RequestStatus status,
        @Param("requesterId") Long requesterId,
        @Param("requestType") RequestType requestType,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.status = :status AND ar.updatedAt >= :since")
    long countByStatusAndUpdatedAtAfter(@Param("status") RequestStatus status, @Param("since") LocalDateTime since);
}
