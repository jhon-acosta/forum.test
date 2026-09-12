package com.forum.api.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.forum.api.model.User;

public record UserResponse(UUID id, String username, Integer maxReplyDepth, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.maxReplyDepth(), user.createdAt());
    }
}
