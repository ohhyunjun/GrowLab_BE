package com.metaverse.growlab_be.article.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.image.dto.ImageResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ArticleResponseDto {
    private Long id;
    private String title;
    private String content;
    private String authorUsername;
    private String category;
    private int viewCount;
    private boolean liked;
    private int likesCount;

    private List<ImageResponseDto> images;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<CommentResponseDto> comments;

    // 1. 상세 조회용
    public ArticleResponseDto(Article article, int likesCount, boolean liked, List<CommentResponseDto> comments) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        // Article 엔티티에서 이미지 리스트를 가져와 ImageResponseDto로 변환하여 저장
        this.images = article.getImages().stream()
                .map(ImageResponseDto::new)
                .collect(Collectors.toList());

        this.likesCount = likesCount;
        this.liked = liked;
        this.comments = comments;
    }

    // 2. 목록 조회용
    public ArticleResponseDto(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        // Article 엔티티에서 이미지 리스트를 가져와 ImageResponseDto로 변환하여 저장
        this.images = article.getImages().stream()
                .map(ImageResponseDto::new)
                .collect(Collectors.toList());

        this.likesCount = 0;
        this.liked = false;
        this.comments = List.of();
    }

    // 3. 좋아요 포함 조회용
    public ArticleResponseDto(Article article, int likesCount, boolean liked) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
        this.category = article.getCategory();
        this.viewCount = article.getViewCount();

        if (article.getUser() != null) {
            this.authorUsername = article.getUser().getUsername();
        }

        // Article 엔티티에서 이미지 리스트를 가져와 ImageResponseDto로 변환하여 저장
        this.images = article.getImages().stream()
                .map(ImageResponseDto::new)
                .collect(Collectors.toList());

        this.likesCount = likesCount;
        this.liked = liked;
        this.comments = List.of();
    }
}