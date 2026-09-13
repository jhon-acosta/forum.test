package com.forum.api.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.forum.api.dto.comment.CommentResponse;
import com.forum.api.dto.discussion.CreateDiscussionRequest;
import com.forum.api.dto.discussion.DiscussionResponse;
import com.forum.api.dto.discussion.DiscussionSummary;
import com.forum.api.dto.user.AuthorResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.Comment;
import com.forum.api.model.Discussion;
import com.forum.api.model.User;
import com.forum.api.repository.CommentRepository;
import com.forum.api.repository.DiscussionRepository;
import com.forum.api.repository.UserRepository;

@Service
public class DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentService commentService;

    public DiscussionService(DiscussionRepository discussionRepository, UserRepository userRepository,
            CommentRepository commentRepository, CommentService commentService) {
        this.discussionRepository = discussionRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.commentService = commentService;
    }

    public DiscussionResponse create(CreateDiscussionRequest request, UUID authorId) {
        User author = findUser(authorId);
        Discussion discussion = new Discussion(
                UUID.randomUUID(),
                request.title().trim(),
                request.content().trim(),
                authorId,
                LocalDateTime.now());
        discussionRepository.save(discussion);
        return toResponse(discussion, author);
    }

    public List<DiscussionSummary> findAll() {
        return discussionRepository.findAll().stream()
                .sorted(Comparator.comparing(Discussion::createdAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    public DiscussionResponse findById(UUID id) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Discussion not found"));
        User author = findUser(discussion.authorId());
        List<CommentResponse> comments = commentService.findTree(discussion.id());
        return toResponse(discussion, author, comments);
    }

    public List<DiscussionSummary> findByAuthorId(UUID authorId) {
        return discussionRepository.findByAuthorId(authorId).stream()
                .sorted(Comparator.comparing(Discussion::createdAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    public List<DiscussionSummary> findParticipating(UUID userId) {
        Set<UUID> discussionIds = commentRepository.findByAuthorId(userId).stream()
                .map(Comment::discussionId)
                .collect(Collectors.toSet());
        return discussionRepository.findAll().stream()
                .filter(d -> discussionIds.contains(d.id()) && !d.authorId().equals(userId))
                .sorted(Comparator.comparing(Discussion::createdAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    private DiscussionSummary toSummary(Discussion discussion) {
        User author = findUser(discussion.authorId());
        long commentCount = commentRepository.findByDiscussionId(discussion.id()).size();
        return new DiscussionSummary(
                discussion.id(),
                discussion.title(),
                discussion.content(),
                AuthorResponse.from(author),
                commentCount,
                discussion.createdAt());
    }

    private DiscussionResponse toResponse(Discussion discussion, User author) {
        return toResponse(discussion, author, List.of());
    }

    private DiscussionResponse toResponse(Discussion discussion, User author, List<CommentResponse> comments) {
        return new DiscussionResponse(
                discussion.id(),
                discussion.title(),
                discussion.content(),
                AuthorResponse.from(author),
                author.maxReplyDepth(),
                discussion.createdAt(),
                comments);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Author not found"));
    }
}
