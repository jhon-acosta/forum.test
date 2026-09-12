package com.forum.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthToken(
        String token,
        UUID userId,
        LocalDateTime createdAt) {
}
