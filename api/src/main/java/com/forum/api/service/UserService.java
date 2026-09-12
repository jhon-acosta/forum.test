package com.forum.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.forum.api.dto.user.SettingsResponse;
import com.forum.api.dto.user.UpdateUserSettingsRequest;
import com.forum.api.dto.user.UserResponse;
import com.forum.api.exception.ApiException;
import com.forum.api.model.User;
import com.forum.api.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    public SettingsResponse getSettings(UUID id) {
        return new SettingsResponse(findUser(id).maxReplyDepth());
    }

    public SettingsResponse updateSettings(UUID id, UpdateUserSettingsRequest request) {
        User user = findUser(id);
        if (!request.isProvided()) {
            return new SettingsResponse(user.maxReplyDepth());
        }

        Integer maxReplyDepth;
        if (request.isUnlimited()) {
            maxReplyDepth = null;
        } else {
            maxReplyDepth = request.getMaxReplyDepth().orElseThrow();
            if (maxReplyDepth < 0) {
                throw ApiException.badRequest("maxReplyDepth must be zero or greater");
            }
        }

        User updated = new User(user.id(), user.username(), user.passwordHash(), maxReplyDepth, user.createdAt());
        userRepository.save(updated);
        return new SettingsResponse(updated.maxReplyDepth());
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
