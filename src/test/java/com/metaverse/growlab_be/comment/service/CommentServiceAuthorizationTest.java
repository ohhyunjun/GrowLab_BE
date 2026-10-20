package com.metaverse.growlab_be.comment.service;

import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.article.service.ArticleService;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.comment.domain.Comment;
import com.metaverse.growlab_be.comment.dto.CommentRequestDto;
import com.metaverse.growlab_be.comment.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentServiceAuthorizationTest {
    @Test
    void onlyTheAuthorCanEditAComment() {
        User author = new User();
        author.setId(1L);
        User otherUser = new User();
        otherUser.setId(2L);
        Comment comment = new Comment();
        comment.setUser(author);
        comment.setContent("original");
        CommentRepository comments = mock(CommentRepository.class);
        when(comments.findById(9L)).thenReturn(Optional.of(comment));
        CommentService service = new CommentService(comments, mock(ArticleRepository.class), mock(ArticleService.class));
        CommentRequestDto request = new CommentRequestDto();
        ReflectionTestUtils.setField(request, "content", "changed");

        assertThatThrownBy(() -> service.updateComment(9L, request, new PrincipalDetails(otherUser)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(comment.getContent()).isEqualTo("original");

        service.updateComment(9L, request, new PrincipalDetails(author));
        assertThat(comment.getContent()).isEqualTo("changed");
    }
}
