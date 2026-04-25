package com.metaverse.growlab_be.article.controller;

import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.service.ArticleService;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
            @RequestPart("articleData") ArticleRequestDto articleRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        ArticleResponseDto articleResponseDto = articleService.createArticle(articleRequestDto, principalDetails, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(articleResponseDto);
    }

    @GetMapping()
    public ResponseEntity<Page<ArticleResponseDto>> getArticles(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ArticleResponseDto> articleResponseDtoPaginationList = articleService.getArticles(pageable, principalDetails);
        return ResponseEntity.ok(articleResponseDtoPaginationList);
    }

    @GetMapping("{articleId}")
    public ResponseEntity<ArticleResponseDto> getArticleById(
            @PathVariable Long articleId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        ArticleResponseDto articleResponseDto = articleService.getArticleById(articleId, principalDetails, request, response);
        return ResponseEntity.ok(articleResponseDto);
    }

//    @GetMapping("/my")
//    public ResponseEntity<Page<ArticleResponseDto>> getMyArticles(
//            @AuthenticationPrincipal PrincipalDetails principalDetails,
//            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
//        Page<ArticleResponseDto> myArticles = articleService.getMyArticle(principalDetails, pageable);
//        return ResponseEntity.ok(myArticles);
//    }

    @PutMapping("{articleId}")
    public ResponseEntity<ArticleResponseDto> updateArticle(
            @PathVariable Long articleId,
            @RequestPart("articleData") ArticleRequestDto articleRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        ArticleResponseDto articleResponseDto = articleService.updateArticle(articleId, articleRequestDto, principalDetails, file);
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
