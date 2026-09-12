package com.forum.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Discussion(
        UUID id,
        String title,
        String content,
        UUID authorId,
        LocalDateTime createdAt) {
}
