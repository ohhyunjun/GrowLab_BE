package com.metaverse.growlab_be.article.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ArticleRequestDto {
    private String title;
    private String content;
    private String category;
    private List<Long> deleteImageIds;
}
