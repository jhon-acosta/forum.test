package com.forum.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String passwordHash,
        Integer maxReplyDepth,
        LocalDateTime createdAt) {
}
