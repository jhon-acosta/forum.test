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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forum.api.dto.comment.CommentResponse;
import com.forum.api.dto.comment.CreateCommentRequest;
import com.forum.api.exception.ApiException;
import com.forum.api.model.Comment;
import com.forum.api.model.Discussion;
import com.forum.api.model.User;
import com.forum.api.repository.CommentRepository;
import com.forum.api.repository.DiscussionRepository;
import com.forum.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(discussionRepository, commentRepository, userRepository);
    }

    private User owner(UUID id, Integer maxDepth) {
        return new User(id, "owner", "hash", maxDepth, LocalDateTime.now());
    }

    private User author(UUID id) {
        return new User(id, "author-" + id, "hash", 3, LocalDateTime.now());
    }

    private Discussion discussion(UUID id, UUID ownerId) {
        return new Discussion(id, "Title", "Content", ownerId, LocalDateTime.now());
    }

    private Comment comment(UUID id, UUID discussionId, UUID parentId, UUID authorId) {
        return new Comment(id, discussionId, parentId, authorId, "content-" + id, LocalDateTime.now());
    }

    @Test
    void createDirectCommentLevelOneIsAllowedWhenMaxDepthIsThree() {
        UUID ownerId = UUID.randomUUID();
        UUID discussionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, ownerId);

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner(ownerId, 3)));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author(authorId)));

        CommentResponse response = commentService.create(discussionId,
                new CreateCommentRequest("Hello", null), authorId);

        assertThat(response.content()).isEqualTo("Hello");
        assertThat(response.parentId()).isNull();
        verify(commentRepository).save(any());
    }

    @Test
    void createReplyLevelTwoAndThreeAreAllowedWithMaxDepthThree() {
        UUID ownerId = UUID.randomUUID();
        UUID discussionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, ownerId);

        UUID c1Id = UUID.randomUUID();
        Comment c1 = comment(c1Id, discussionId, null, authorId);
        UUID c2Id = UUID.randomUUID();
        Comment c2 = comment(c2Id, discussionId, c1Id, authorId);

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner(ownerId, 3)));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author(authorId)));
        when(commentRepository.findById(c1Id)).thenReturn(Optional.of(c1));
        when(commentRepository.findById(c2Id)).thenReturn(Optional.of(c2));

        // reply to c1 -> level 2
        CommentResponse r2 = commentService.create(discussionId,
                new CreateCommentRequest("reply to c1", c1Id), authorId);
        assertThat(r2.parentId()).isEqualTo(c1Id);

        // reply to c2 -> level 3
        CommentResponse r3 = commentService.create(discussionId,
                new CreateCommentRequest("reply to c2", c2Id), authorId);
        assertThat(r3.parentId()).isEqualTo(c2Id);
    }

    @Test
    void createExceedingMaxDepthIsRejected() {
        UUID ownerId = UUID.randomUUID();
        UUID discussionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, ownerId);

        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();
        UUID c3Id = UUID.randomUUID();
        Comment c1 = comment(c1Id, discussionId, null, authorId);
        Comment c2 = comment(c2Id, discussionId, c1Id, authorId);
        Comment c3 = comment(c3Id, discussionId, c2Id, authorId);

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner(ownerId, 3)));
        when(commentRepository.findById(c3Id)).thenReturn(Optional.of(c3));
        when(commentRepository.findById(c2Id)).thenReturn(Optional.of(c2));
        when(commentRepository.findById(c1Id)).thenReturn(Optional.of(c1));

        assertThatThrownBy(() -> commentService.create(discussionId,
                new CreateCommentRequest("too deep", c3Id), authorId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Maximum reply depth exceeded");
    }

    @Test
    void unlimitedDepthAllowsDeepNesting() {
        UUID ownerId = UUID.randomUUID();
        UUID discussionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, ownerId);

        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();
        UUID c3Id = UUID.randomUUID();
        Comment c1 = comment(c1Id, discussionId, null, authorId);
        Comment c2 = comment(c2Id, discussionId, c1Id, authorId);
        Comment c3 = comment(c3Id, discussionId, c2Id, authorId);

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner(ownerId, null)));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author(authorId)));
        when(commentRepository.findById(c3Id)).thenReturn(Optional.of(c3));
        when(commentRepository.findById(c2Id)).thenReturn(Optional.of(c2));
        when(commentRepository.findById(c1Id)).thenReturn(Optional.of(c1));

        CommentResponse response = commentService.create(discussionId,
                new CreateCommentRequest("deep unlimited", c3Id), authorId);

        assertThat(response.parentId()).isEqualTo(c3Id);
    }

    @Test
    void parentNotFoundIsRejected() {
        UUID discussionId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, UUID.randomUUID());
        UUID parentId = UUID.randomUUID();

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(commentRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(discussionId,
                new CreateCommentRequest("reply", parentId), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Parent comment not found");
    }

    @Test
    void parentFromOtherDiscussionIsRejected() {
        UUID discussionId = UUID.randomUUID();
        UUID otherDiscussionId = UUID.randomUUID();
        Discussion discussion = discussion(discussionId, UUID.randomUUID());
        UUID parentId = UUID.randomUUID();
        Comment parent = comment(parentId, otherDiscussionId, null, UUID.randomUUID());

        when(discussionRepository.findById(discussionId)).thenReturn(Optional.of(discussion));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.create(discussionId,
                new CreateCommentRequest("reply", parentId), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Parent comment does not belong to this discussion");
    }

    @Test
    void discussionNotFoundIsRejected() {
        UUID discussionId = UUID.randomUUID();
        when(discussionRepository.findById(discussionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(discussionId,
                new CreateCommentRequest("hello", null), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Discussion not found");
    }

    @Test
    void buildTreeAssemblesHierarchy() {
        UUID discussionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User author = author(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));

        UUID root1 = UUID.randomUUID();
        UUID root2 = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Comment cRoot1 = new Comment(root1, discussionId, null, authorId, "root1", now.minusMinutes(2));
        Comment cRoot2 = new Comment(root2, discussionId, null, authorId, "root2", now.minusMinutes(1));
        Comment cChild = new Comment(child, discussionId, root1, authorId, "child", now);

        List<CommentResponse> tree = commentService.buildTree(List.of(cRoot1, cRoot2, cChild));

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).id()).isEqualTo(root1);
        assertThat(tree.get(0).replies()).hasSize(1);
        assertThat(tree.get(0).replies().get(0).id()).isEqualTo(child);
        assertThat(tree.get(1).id()).isEqualTo(root2);
        assertThat(tree.get(1).replies()).isEmpty();
    }
}
