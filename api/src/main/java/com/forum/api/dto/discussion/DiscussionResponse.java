package com.forum.api.dto.discussion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.forum.api.dto.comment.CommentResponse;
import com.forum.api.dto.user.AuthorResponse;

public record DiscussionResponse(
        UUID id,
        String title,
        String content,
        AuthorResponse author,
        LocalDateTime createdAt,
        List<CommentResponse> comments) {
}
