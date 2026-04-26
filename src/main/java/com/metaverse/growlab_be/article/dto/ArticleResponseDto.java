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
    private String imageUrl;
    private String category;
    private int viewCount;
    private boolean liked;
    private int likesCount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<CommentResponseDto> comments;

    public ArticleResponseDto(Article article, int likesCount, boolean liked, List<CommentResponseDto> comments) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.imageUrl = article.getImageUrl();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        this.likesCount = likesCount;
        this.liked = liked;
        this.comments = comments;
    }

    public ArticleResponseDto(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.imageUrl = article.getImageUrl();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        this.likesCount = 0;
        this.liked = false;
        this.comments = List.of();
    }

    public ArticleResponseDto(Article article, int likesCount, boolean liked) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
        this.imageUrl = article.getImageUrl();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        this.likesCount = likesCount;
        this.liked = liked;
        this.comments = List.of();
    }
}
