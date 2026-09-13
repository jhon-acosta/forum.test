package com.forum.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forum.api.dto.discussion.CreateDiscussionRequest;
import com.forum.api.dto.discussion.DiscussionResponse;
import com.forum.api.dto.discussion.DiscussionSummary;
import com.forum.api.exception.ApiException;
import com.forum.api.model.Discussion;
import com.forum.api.model.User;
import com.forum.api.repository.CommentRepository;
import com.forum.api.repository.DiscussionRepository;
import com.forum.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceTest {

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentService commentService;

    private DiscussionService discussionService() {
        return new DiscussionService(discussionRepository, userRepository, commentRepository, commentService);
    }

    private User user(UUID id, String username) {
        return new User(id, username, "hash", 3, LocalDateTime.now().minusDays(1));
    }

    private Discussion discussion(UUID id, UUID authorId, LocalDateTime createdAt) {
        return new Discussion(id, "Title " + id, "Content " + id, authorId, createdAt);
    }

    @Test
    void createSavesDiscussionAndReturnsResponse() {
        UUID authorId = UUID.randomUUID();
        User author = user(authorId, "jhon");
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));

        DiscussionService service = discussionService();
        DiscussionResponse response = service.create(new CreateDiscussionRequest("Hello", "World"), authorId);

        ArgumentCaptor<Discussion> captor = ArgumentCaptor.forClass(Discussion.class);
        verify(discussionRepository).save(captor.capture());
        Discussion saved = captor.getValue();
        assertThat(saved.title()).isEqualTo("Hello");
        assertThat(saved.authorId()).isEqualTo(authorId);
        assertThat(response.title()).isEqualTo("Hello");
        assertThat(response.author().username()).isEqualTo("jhon");
        assertThat(response.maxReplyDepth()).isEqualTo(3);
        assertThat(response.comments()).isEmpty();
    }

    @Test
    void findAllReturnsSummariesSortedByCreatedAtDescWithCommentCount() {
        UUID authorId = UUID.randomUUID();
        User author = user(authorId, "jhon");
        LocalDateTime now = LocalDateTime.now();
        Discussion older = discussion(UUID.randomUUID(), authorId, now.minusDays(2));
        Discussion newer = discussion(UUID.randomUUID(), authorId, now);

        when(discussionRepository.findAll()).thenReturn(List.of(older, newer));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.findByDiscussionId(older.id())).thenReturn(List.of(
                new com.forum.api.model.Comment(UUID.randomUUID(), older.id(), null, authorId, "c", now),
                new com.forum.api.model.Comment(UUID.randomUUID(), older.id(), null, authorId, "c2", now)));
        when(commentRepository.findByDiscussionId(newer.id())).thenReturn(List.of());

        List<DiscussionSummary> result = discussionService().findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(newer.id());
        assertThat(result.get(0).commentCount()).isZero();
        assertThat(result.get(1).id()).isEqualTo(older.id());
        assertThat(result.get(1).commentCount()).isEqualTo(2);
    }

    @Test
    void findByIdReturnsDiscussion() {
        UUID authorId = UUID.randomUUID();
        User author = user(authorId, "jhon");
        Discussion discussion = discussion(UUID.randomUUID(), authorId, LocalDateTime.now());

        when(discussionRepository.findById(discussion.id())).thenReturn(Optional.of(discussion));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentService.findTree(discussion.id())).thenReturn(List.of());

        DiscussionResponse response = discussionService().findById(discussion.id());

        assertThat(response.id()).isEqualTo(discussion.id());
        assertThat(response.author().username()).isEqualTo("jhon");
        assertThat(response.maxReplyDepth()).isEqualTo(3);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(discussionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discussionService().findById(id))
                .isInstanceOf(ApiException.class)
                .hasMessage("Discussion not found");
    }

    @Test
    void findByAuthorIdReturnsOwnDiscussions() {
        UUID authorId = UUID.randomUUID();
        User author = user(authorId, "jhon");
        Discussion discussion = discussion(UUID.randomUUID(), authorId, LocalDateTime.now());

        when(discussionRepository.findByAuthorId(authorId)).thenReturn(List.of(discussion));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.findByDiscussionId(discussion.id())).thenReturn(List.of());

        List<DiscussionSummary> result = discussionService().findByAuthorId(authorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(discussion.id());
    }
}
