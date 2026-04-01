package com.metaverse.growlab_be.likes.articleLike.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArticleLikeResponseDto {
    private Long articleId;
    private Long userId;
    private boolean liked;
    private int likesCount;

    public ArticleLikeResponseDto(Long articleId, Long userId, boolean liked, int likesCount) {
        this.articleId = articleId;
        this.userId = userId;
        this.liked = liked;
        this.likesCount = likesCount;
    }
}
