package com.metaverse.growlab_be.comment.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.article.service.ArticleService;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.comment.domain.Comment;
import com.metaverse.growlab_be.comment.dto.CommentRequestDto;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleService articleService;

    @Transactional
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, Long articleId, PrincipalDetails principalDetails) {
        User logginedUser = principalDetails.user();
        Article foundArticle = articleService.getValidArticleById(articleId);
        Comment newComment = new Comment(commentRequestDto.getContent(), foundArticle, logginedUser);
        Comment savedComment = commentRepository.save(newComment);
        CommentResponseDto commentResponseDto = new CommentResponseDto(savedComment);
        return commentResponseDto;
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByArticle(Long articleId) {
        List<CommentResponseDto> commentResponseDtoList = commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(CommentResponseDto::new).toList();
        return commentResponseDtoList;
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments() {
        List<CommentResponseDto> commentResponseDtoList = commentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CommentResponseDto::new).toList();
        return commentResponseDtoList;
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getMyComments(Long userId) {
        List<CommentResponseDto> commentResponseDtoList = commentRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CommentResponseDto::new).toList();
        return commentResponseDtoList;
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto) {
        Comment foundComment = getValidCommentById(commentId);
        foundComment.update(commentRequestDto);
        return new CommentResponseDto(foundComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment foundComment = getValidCommentById(commentId);
        commentRepository.delete(foundComment);
    }

    public Comment getValidCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() ->
                new IllegalArgumentException("선택한 id의 댓글은 존재하지 않습니다."));
    }

    public Article getValidArticleById(Long id) {
        return articleRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("선택한 id의 게시글은 존재하지 않습니다."));
    }

    public CommentResponseDto getCommentById(Long articleId, Long commentId, PrincipalDetails principalDetails) {
        Comment comment = getValidComment(articleId, commentId);
        return new CommentResponseDto(comment);
    }

    public Comment getValidComment(Long articleId, Long commentId) {
        articleService.getValidArticleById(articleId);

        return commentRepository.findByIdAndArticleId(commentId, articleId).orElseThrow(() ->
                new IllegalArgumentException("게시글(ID: " + articleId + ")에서 댓글(ID: " + commentId + ")을 찾을 수 없습니다.")
        );
    }
}