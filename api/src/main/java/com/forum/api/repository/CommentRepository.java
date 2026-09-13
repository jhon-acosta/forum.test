package com.forum.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.forum.api.config.ForumProperties;
import com.forum.api.model.Comment;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class CommentRepository extends JsonFileRepository<Comment, UUID> {

    public CommentRepository(JsonMapper mapper, ForumProperties properties) {
        super(properties.dataDir().resolve("comments.json"), Comment.class, Comment::id, mapper);
    }

    public List<Comment> findByDiscussionId(UUID discussionId) {
        return findAll().stream().filter(comment -> comment.discussionId().equals(discussionId)).toList();
    }

    public List<Comment> findByAuthorId(UUID authorId) {
        return findAll().stream().filter(comment -> comment.authorId().equals(authorId)).toList();
    }
}
