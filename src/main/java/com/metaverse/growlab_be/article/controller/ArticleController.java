package com.metaverse.growlab_be.article.controller;

import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    @PostMapping()
    public ResponseEntity<ArticleResponseDto> createArticle(@RequestBody ArticleRequestDto articleRequestDto) {
        ArticleResponseDto articleResponseDto = articleService.createArticle(articleRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(articleResponseDto);
    }

    @GetMapping()
    public ResponseEntity<List<ArticleResponseDto>> getArticles() {
        List<ArticleResponseDto> articleResponseDtoList = articleService.getArticles();
        return ResponseEntity.ok(articleResponseDtoList);
    }

    @GetMapping("{articleId}")
    public ResponseEntity<ArticleResponseDto> getArticleById(@PathVariable Long articleId) {
        ArticleResponseDto articleResponseDto = articleService.getArticleById(articleId);
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
}
