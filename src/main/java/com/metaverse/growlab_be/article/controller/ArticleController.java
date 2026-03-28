package com.metaverse.growlab_be.article.controller;

import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.service.ArticleService;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    @PostMapping()
    public ResponseEntity<ArticleResponseDto> createArticle(
            @RequestBody ArticleRequestDto articleRequestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        ArticleResponseDto articleResponseDto = articleService.createArticle(articleRequestDto, principalDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(articleResponseDto);
    }

    @GetMapping()
    public ResponseEntity<Page<ArticleResponseDto>> getArticles(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ArticleResponseDto> articleResponseDtoPaginationList = articleService.getArticles(pageable);
        return ResponseEntity.ok(articleResponseDtoPaginationList);
    }

    @GetMapping("{articleId}")
    public ResponseEntity<ArticleResponseDto> getArticleById(
            @PathVariable Long articleId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        ArticleResponseDto articleResponseDto = articleService.getArticleById(articleId, principalDetails);
        return ResponseEntity.ok(articleResponseDto);
    }

    @PutMapping("{articleId}")
    public ResponseEntity<ArticleResponseDto> updateArticle(@PathVariable Long articleId, @RequestBody ArticleRequestDto articleRequestDto) {
        ArticleResponseDto articleResponseDto = articleService.updateArticle(articleId, articleRequestDto);
        return ResponseEntity.ok(articleResponseDto);
    }

    @DeleteMapping("{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long articleId) {
        articleService.deleteArticle(articleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("liked")
    public ResponseEntity<List<ArticleResponseDto>> getLikedArticles(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<ArticleResponseDto> articleResponseDtoList = articleService.getLikedArticles(pageable, principalDetails);
        return ResponseEntity.ok(articleResponseDtoList);
    }
}
