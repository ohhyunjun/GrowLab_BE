package com.metaverse.growlab_be.article.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArticleRequestDto {
    private String title;
    private String content;
    private String imageUrl;
    private String category;
}
