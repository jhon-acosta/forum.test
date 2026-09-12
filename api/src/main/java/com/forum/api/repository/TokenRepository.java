package com.forum.api.repository;

import org.springframework.stereotype.Repository;

import com.forum.api.config.ForumProperties;
import com.forum.api.model.AuthToken;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class TokenRepository extends JsonFileRepository<AuthToken, String> {

    public TokenRepository(JsonMapper mapper, ForumProperties properties) {
        super(properties.dataDir().resolve("tokens.json"), AuthToken.class, AuthToken::token, mapper);
    }
}
