package com.metaverse.growlab_be.comment.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.comment.dto.CommentRequestDto;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    // 1. 댓글 작성
    @PostMapping("/articles/{articleId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @RequestBody CommentRequestDto commentRequestDto,
            @PathVariable Long articleId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        CommentResponseDto commentResponseDto = commentService.createComment(commentRequestDto, articleId, principalDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentResponseDto);
    }

    @GetMapping("/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> getCommentById(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        CommentResponseDto commentResponseDto = commentService.getCommentById(articleId, commentId, principalDetails);
        return ResponseEntity.ok(commentResponseDto);
    }

    // 2. 게시글 댓글 조회
    @GetMapping("/articles/{articleId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByArticle(@PathVariable Long articleId) {
        List<CommentResponseDto> commentResponseDtoList = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(commentResponseDtoList);
    }

    // 3. 전체 댓글 조회
    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments() {
        List<CommentResponseDto> commentResponseDtoList = commentService.getComments();
        return ResponseEntity.ok(commentResponseDtoList);
    }

    // 4. 내가 쓴 댓글 조회
    @GetMapping("/comments/my")
    public ResponseEntity<List<CommentResponseDto>> getMyComments(@RequestParam Long userId) {
        List<CommentResponseDto> commentResponseDtoList = commentService.getMyComments(userId);
        return ResponseEntity.ok(commentResponseDtoList);
    }

    // 5. 댓글 수정
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto commentRequestDto) {
        CommentResponseDto commentResponseDto = commentService.updateComment(commentId, commentRequestDto);
        return ResponseEntity.ok(commentResponseDto);
    }

    // 6. 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
