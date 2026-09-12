package com.forum.api.dto.auth;

import com.forum.api.dto.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
