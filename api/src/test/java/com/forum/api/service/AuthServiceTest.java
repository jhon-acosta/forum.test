package com.forum.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.forum.api.config.ForumProperties;
import com.forum.api.dto.auth.AuthResponse;
import com.forum.api.dto.auth.LoginRequest;
import com.forum.api.dto.auth.RegisterRequest;
import com.forum.api.dto.user.UserResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.AuthToken;
import com.forum.api.model.User;
import com.forum.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        ForumProperties properties = new ForumProperties(Path.of("target/test-data"), 3, List.of());
        authService = new AuthService(userRepository, passwordEncoder, tokenService, properties);
    }

    private User user(String username, String hash) {
        return new User(UUID.randomUUID(), username, hash, 3, LocalDateTime.now());
    }

    @Test
    void registerHashesPasswordAndUsesDefaultDepth() {
        when(userRepository.existsByUsername("jhon")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");

        UserResponse response = authService.register(new RegisterRequest("jhon", "secret123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.passwordHash()).isNotEqualTo("secret123");
        assertThat(saved.maxReplyDepth()).isEqualTo(3);
        assertThat(response.username()).isEqualTo("jhon");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("jhon")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jhon", "secret123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        User user = user("jhon", "bcrypt-hash");
        when(userRepository.findByUsername("jhon")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "bcrypt-hash")).thenReturn(true);
        when(tokenService.issue(user.id()))
                .thenReturn(new AuthToken("token-1", user.id(), LocalDateTime.now()));

        AuthResponse response = authService.login(new LoginRequest("jhon", "secret123"));

        assertThat(response.token()).isEqualTo("token-1");
        assertThat(response.user().username()).isEqualTo("jhon");
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "secret123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = user("jhon", "bcrypt-hash");
        when(userRepository.findByUsername("jhon")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jhon", "wrong")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void logoutRevokesTokenFromHeader() {
        authService.logout("Bearer token-1");

        verify(tokenService).revoke("token-1");
    }

    @Test
    void logoutWithoutHeaderDoesNothing() {
        authService.logout(null);

        verifyNoInteractions(tokenService);
    }
}
