package com.metaverse.growlab_be.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.comment.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private Long articleId;
    private String authorUsername;
    private String authorNickname;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();

        if (comment.getUser() != null) {
            this.authorUsername = comment.getUser().getUsername();
        }

        if (comment.getArticle() != null) {
            this.articleId = comment.getArticle().getId();
        } else {
            this.articleId = null;
        }
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }
}
