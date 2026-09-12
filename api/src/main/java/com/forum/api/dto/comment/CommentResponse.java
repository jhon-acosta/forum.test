package com.forum.api.dto.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.forum.api.dto.user.AuthorResponse;

public record CommentResponse(
        UUID id,
        UUID parentId,
        String content,
        AuthorResponse author,
        LocalDateTime createdAt,
        List<CommentResponse> replies) {
}
