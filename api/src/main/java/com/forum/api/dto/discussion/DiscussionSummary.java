package com.forum.api.dto.discussion;

import java.time.LocalDateTime;
import java.util.UUID;

import com.forum.api.dto.user.AuthorResponse;

public record DiscussionSummary(
        UUID id,
        String title,
        String content,
        AuthorResponse author,
        long commentCount,
        LocalDateTime createdAt) {
}
