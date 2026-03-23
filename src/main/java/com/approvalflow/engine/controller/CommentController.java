package com.approvalflow.engine.controller;

import com.approvalflow.engine.dto.request.CreateCommentDto;
import com.approvalflow.engine.dto.response.CommentResponse;
import com.approvalflow.engine.dto.response.PagedResponse;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.service.CommentService;
import com.approvalflow.engine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests/{requestId}/comments")
public class CommentController {
    
    private final CommentService commentService;
    private final UserService userService;
    
    public CommentController(CommentService commentService,
                            UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }
    
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateCommentDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        CommentResponse response = commentService.addComment(requestId, dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<CommentResponse>> getComments(
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<CommentResponse> response = commentService.getComments(requestId, pageable);
        return ResponseEntity.ok(response);
    }
}
