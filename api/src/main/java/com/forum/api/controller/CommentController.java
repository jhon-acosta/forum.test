package com.forum.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.api.dto.comment.CommentResponse;
import com.forum.api.dto.comment.CreateCommentRequest;
import com.forum.api.model.User;
import com.forum.api.service.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/discussions/{discussionId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID discussionId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User user) {
        return commentService.create(discussionId, request, user.id());
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID discussionId) {
        return commentService.findTree(discussionId);
    }
}
