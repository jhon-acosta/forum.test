package com.forum.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forum.api.dto.user.SettingsResponse;
import com.forum.api.dto.user.UpdateUserSettingsRequest;
import com.forum.api.dto.user.UserResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.User;
import com.forum.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    private User user(Integer maxReplyDepth) {
        return new User(UUID.randomUUID(), "jhon", "bcrypt-hash", maxReplyDepth, LocalDateTime.now());
    }

    private UpdateUserSettingsRequest request(Integer value, boolean unlimited) {
        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest();
        request.setMaxReplyDepth(unlimited ? Optional.empty() : Optional.ofNullable(value));
        return request;
    }

    @Test
    void getByIdReturnsUser() {
        User user = user(3);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(user.id());

        assertThat(response.id()).isEqualTo(user.id());
        assertThat(response.username()).isEqualTo("jhon");
        assertThat(response.maxReplyDepth()).isEqualTo(3);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateSettingsChangesDepth() {
        User user = user(3);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        SettingsResponse response = userService.updateSettings(user.id(), request(5, false));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().maxReplyDepth()).isEqualTo(5);
        assertThat(response.maxReplyDepth()).isEqualTo(5);
        assertThat(captor.getValue().passwordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void updateSettingsWithNullMeansUnlimited() {
        User user = user(3);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        SettingsResponse response = userService.updateSettings(user.id(), request(null, true));

        assertThat(response.maxReplyDepth()).isNull();
        verify(userRepository).save(any());
    }

    @Test
    void updateSettingsAbsentFieldKeepsCurrent() {
        User user = user(3);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        SettingsResponse response = userService.updateSettings(user.id(), new UpdateUserSettingsRequest());

        assertThat(response.maxReplyDepth()).isEqualTo(3);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateSettingsRejectsNegativeDepth() {
        User user = user(3);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateSettings(user.id(), request(-1, false)))
                .isInstanceOf(ApiException.class)
                .hasMessage("maxReplyDepth must be zero or greater");
        verify(userRepository, never()).save(any());
    }
}
