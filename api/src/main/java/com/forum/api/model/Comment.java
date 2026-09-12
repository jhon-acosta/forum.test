package com.forum.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Comment(
        UUID id,
        UUID discussionId,
        UUID parentId,
        UUID authorId,
        String content,
        LocalDateTime createdAt) {
}
