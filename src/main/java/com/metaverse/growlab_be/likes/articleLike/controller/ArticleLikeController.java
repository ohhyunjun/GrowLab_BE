package com.metaverse.growlab_be.likes.articleLike.controller;

import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.likes.articleLike.dto.ArticleLikeResponseDto;
import com.metaverse.growlab_be.likes.articleLike.service.ArticleLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/articles")
@RequiredArgsConstructor
public class ArticleLikeController {
    private final ArticleLikeService articleLikeService;

    @PostMapping("{articleId}/likes")
    public ResponseEntity<ArticleLikeResponseDto> toggleLikeArticle(
            @PathVariable Long articleId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        ArticleLikeResponseDto response = articleLikeService.toggleArticleLike(principalDetails, articleId);
        return ResponseEntity.ok(response);
    }
}
