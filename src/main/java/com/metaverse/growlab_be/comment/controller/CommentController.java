package com.metaverse.growlab_be.comment.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.comment.dto.CommentRequestDto;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

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

    @GetMapping("/articles/{articleId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByArticle(@PathVariable Long articleId) {
        List<CommentResponseDto> commentResponseDtoList = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(commentResponseDtoList);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments() {
        List<CommentResponseDto> commentResponseDtoList = commentService.getComments();
        return ResponseEntity.ok(commentResponseDtoList);
    }

    @GetMapping("/comments/my")
    public ResponseEntity<List<CommentResponseDto>> getMyComments(@RequestParam Long userId) {
        List<CommentResponseDto> commentResponseDtoList = commentService.getMyComments(userId);
        return ResponseEntity.ok(commentResponseDtoList);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto commentRequestDto) {
        CommentResponseDto commentResponseDto = commentService.updateComment(commentId, commentRequestDto);
        return ResponseEntity.ok(commentResponseDto);
    }

    // ✅ 일반 삭제 - 본인 댓글만 가능
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        commentService.deleteComment(commentId, principalDetails);
        return ResponseEntity.noContent().build();
    }

    // ✅ (관리자) 강제 삭제 - 소유자 무관
    @DeleteMapping("/admin/comments/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCommentByAdmin(@PathVariable Long commentId) {
        try {
            commentService.deleteCommentByAdmin(commentId);
            return ResponseEntity.ok("댓글이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}