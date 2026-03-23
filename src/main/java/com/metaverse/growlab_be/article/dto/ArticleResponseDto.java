package com.metaverse.growlab_be.article.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ArticleResponseDto {
    private Long id;
    private String title;
    private String content;
    private String authorUsername;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<CommentResponseDto> comments;

    public ArticleResponseDto(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        if (article.getComments() != null) {
            this.comments = article.getComments().stream().map(CommentResponseDto::new).toList();
        } else {
            this.comments = List.of();
        }
    }
}
