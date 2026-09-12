package com.forum.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.forum.api.config.ForumProperties;
import com.forum.api.model.Discussion;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class DiscussionRepository extends JsonFileRepository<Discussion, UUID> {

    public DiscussionRepository(JsonMapper mapper, ForumProperties properties) {
        super(properties.dataDir().resolve("discussions.json"), Discussion.class, Discussion::id, mapper);
    }

    public List<Discussion> findByAuthorId(UUID authorId) {
        return findAll().stream().filter(discussion -> discussion.authorId().equals(authorId)).toList();
    }
}
