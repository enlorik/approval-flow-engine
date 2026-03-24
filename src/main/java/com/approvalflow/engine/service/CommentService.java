package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.request.CreateCommentDto;
import com.approvalflow.engine.dto.response.CommentResponse;
import com.approvalflow.engine.dto.response.PagedResponse;
import com.approvalflow.engine.entity.ApprovalRequest;
import com.approvalflow.engine.entity.Comment;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.AuditAction;
import com.approvalflow.engine.exception.ResourceNotFoundException;
import com.approvalflow.engine.repository.ApprovalRequestRepository;
import com.approvalflow.engine.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final ApprovalRequestRepository requestRepository;
    private final AuditLogService auditLogService;
    
    public CommentService(CommentRepository commentRepository,
                         ApprovalRequestRepository requestRepository,
                         AuditLogService auditLogService) {
        this.commentRepository = commentRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public CommentResponse addComment(Long requestId, CreateCommentDto dto, User author) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));
        
        Comment comment = Comment.builder()
                .approvalRequest(request)
                .author(author)
                .content(dto.getContent())
                .build();
        
        comment = commentRepository.save(comment);
        
        auditLogService.logAction(author, AuditAction.COMMENT_ADDED, 
                "Comment", comment.getId(), 
                "Comment added to request: " + request.getTitle());
        
        return mapToResponse(comment);
    }
    
    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getComments(Long requestId, Pageable pageable) {
        if (!requestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException("ApprovalRequest", "id", requestId);
        }
        
        Page<Comment> page = commentRepository.findByApprovalRequestId(requestId, pageable);
        
        List<CommentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<CommentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    
    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .requestId(comment.getApprovalRequest().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
