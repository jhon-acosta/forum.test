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

import com.forum.api.dto.discussion.CreateDiscussionRequest;
import com.forum.api.dto.discussion.DiscussionResponse;
import com.forum.api.dto.discussion.DiscussionSummary;
import com.forum.api.model.User;
import com.forum.api.service.DiscussionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    public List<DiscussionSummary> list() {
        return discussionService.findAll();
    }

    @GetMapping("/{id}")
    public DiscussionResponse get(@PathVariable UUID id) {
        return discussionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscussionResponse create(@Valid @RequestBody CreateDiscussionRequest request,
            @AuthenticationPrincipal User user) {
        return discussionService.create(request, user.id());
    }
}
