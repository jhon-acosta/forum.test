package com.forum.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.forum.api.config.ForumProperties;
import com.forum.api.model.User;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class UserRepository extends JsonFileRepository<User, UUID> {

    public UserRepository(JsonMapper mapper, ForumProperties properties) {
        super(properties.dataDir().resolve("users.json"), User.class, User::id, mapper);
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream().filter(user -> user.username().equals(username)).findFirst();
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}
