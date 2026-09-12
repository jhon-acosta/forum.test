package com.forum.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.forum.api.dto.comment.CommentResponse;
import com.forum.api.dto.comment.CreateCommentRequest;
import com.forum.api.dto.user.AuthorResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.Comment;
import com.forum.api.model.Discussion;
import com.forum.api.model.User;
import com.forum.api.repository.CommentRepository;
import com.forum.api.repository.DiscussionRepository;
import com.forum.api.repository.UserRepository;

@Service
public class CommentService {

    private final DiscussionRepository discussionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(DiscussionRepository discussionRepository, CommentRepository commentRepository,
            UserRepository userRepository) {
        this.discussionRepository = discussionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public CommentResponse create(UUID discussionId, CreateCommentRequest request, UUID authorId) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> ApiException.notFound("Discussion not found"));

        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> ApiException.notFound("Parent comment not found"));
            if (!parent.discussionId().equals(discussionId)) {
                throw ApiException.badRequest("Parent comment does not belong to this discussion");
            }
        }

        User owner = userRepository.findById(discussion.authorId())
                .orElseThrow(() -> ApiException.notFound("Discussion owner not found"));
        Integer maxDepth = owner.maxReplyDepth();

        int newLevel = computeNewLevel(request.parentId());
        if (maxDepth != null && newLevel > maxDepth) {
            throw ApiException.unprocessable("Maximum reply depth exceeded");
        }

        Comment comment = new Comment(
                UUID.randomUUID(),
                discussionId,
                request.parentId(),
                authorId,
                request.content().trim(),
                LocalDateTime.now());
        commentRepository.save(comment);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> ApiException.notFound("Author not found"));
        return toResponse(comment, author);
    }

    public List<CommentResponse> findTree(UUID discussionId) {
        List<Comment> comments = commentRepository.findByDiscussionId(discussionId);
        return buildTree(comments);
    }

    List<CommentResponse> buildTree(List<Comment> comments) {
        Map<UUID, List<Comment>> childrenByParent = new HashMap<>();
        for (Comment comment : comments) {
            childrenByParent.computeIfAbsent(comment.parentId(), key -> new ArrayList<>()).add(comment);
        }

        childrenByParent.values().forEach(list -> list.sort(Comparator.comparing(Comment::createdAt)));

        return buildBranch(null, childrenByParent);
    }

    private List<CommentResponse> buildBranch(UUID parentId, Map<UUID, List<Comment>> childrenByParent) {
        List<Comment> children = childrenByParent.getOrDefault(parentId, List.of());
        List<CommentResponse> result = new ArrayList<>();
        for (Comment child : children) {
            User author = userRepository.findById(child.authorId())
                    .orElseThrow(() -> ApiException.notFound("Author not found"));
            List<CommentResponse> replies = buildBranch(child.id(), childrenByParent);
            result.add(new CommentResponse(
                    child.id(),
                    child.parentId(),
                    child.content(),
                    AuthorResponse.from(author),
                    child.createdAt(),
                    replies));
        }
        return result;
    }

    private int computeNewLevel(UUID parentId) {
        if (parentId == null) {
            return 1;
        }
        return computeLevel(parentId) + 1;
    }

    private int computeLevel(UUID commentId) {
        int level = 1;
        Set<UUID> visited = new HashSet<>();
        UUID currentParentId = null;

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Parent comment not found"));
        currentParentId = comment.parentId();

        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw ApiException.badRequest("Cycle detected in comment chain");
            }
            if (visited.size() > 1000) {
                throw ApiException.badRequest("Comment chain too deep / possible cycle");
            }
            Optional<Comment> parent = commentRepository.findById(currentParentId);
            if (parent.isEmpty()) {
                break;
            }
            level++;
            currentParentId = parent.get().parentId();
        }
        return level;
    }

    private CommentResponse toResponse(Comment comment, User author) {
        return new CommentResponse(
                comment.id(),
                comment.parentId(),
                comment.content(),
                AuthorResponse.from(author),
                comment.createdAt(),
                List.of());
    }
}
