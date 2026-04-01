package com.metaverse.growlab_be.likes.articleLike.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.likes.articleLike.dto.ArticleLikeResponseDto;
import com.metaverse.growlab_be.likes.articleLike.service.ArticleLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
