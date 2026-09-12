package com.forum.api.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.forum.api.model.AuthToken;
import com.forum.api.repository.TokenRepository;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public AuthToken issue(UUID userId) {
        AuthToken authToken = new AuthToken(UUID.randomUUID().toString(), userId, LocalDateTime.now());
        return tokenRepository.save(authToken);
    }

    public Optional<AuthToken> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findById(token);
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            tokenRepository.deleteById(token);
        }
    }
}
