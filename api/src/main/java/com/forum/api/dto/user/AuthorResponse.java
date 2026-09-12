package com.forum.api.dto.user;

import java.util.UUID;

import com.forum.api.model.User;

public record AuthorResponse(UUID id, String username) {

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.id(), user.username());
    }
}
