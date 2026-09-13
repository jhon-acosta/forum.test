package com.forum.api.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.forum.api.config.ForumProperties;
import com.forum.api.dto.auth.AuthResponse;
import com.forum.api.dto.auth.LoginRequest;
import com.forum.api.dto.auth.RegisterRequest;
import com.forum.api.dto.user.UserResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.AuthToken;
import com.forum.api.model.User;
import com.forum.api.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ForumProperties properties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            ForumProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    public UserResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (!username.matches("^[a-z0-9._-]{3,30}$")) {
            throw ApiException.badRequest(
                    "El nombre de usuario solo permite letras, números, punto, guion y guion bajo, sin espacios");
        }
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Username already exists");
        }
        User user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(request.password()),
                properties.defaultMaxReplyDepth(),
                LocalDateTime.now());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        AuthToken token = tokenService.issue(user.id());
        return new AuthResponse(token.token(), UserResponse.from(user));
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            tokenService.revoke(authorizationHeader.substring("Bearer ".length()));
        }
    }

    private String normalizeUsername(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
