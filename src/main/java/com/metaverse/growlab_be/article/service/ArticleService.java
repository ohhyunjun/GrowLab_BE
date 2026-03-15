package com.metaverse.growlab_be.article.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;

    @Transactional
    public ArticleResponseDto createArticle(ArticleRequestDto articleRequestDto) {
        Article newArticle = new Article(articleRequestDto);
        Article savedArticle = articleRepository.save(newArticle);
        ArticleResponseDto articleResponseDto = new ArticleResponseDto(savedArticle);
        return articleResponseDto;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponseDto> getArticles() {
        List<ArticleResponseDto> articleResponseDto = articleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ArticleResponseDto::new).toList();
        return articleResponseDto;
    }

    @Transactional(readOnly = true)
    public ArticleResponseDto getArticleById(Long articleId) {
        Article foundArticle = findArticleById(articleId);
        ArticleResponseDto articleResponseDto = new ArticleResponseDto(foundArticle);
        return articleResponseDto;
    }

    @Transactional
    public ArticleResponseDto updateArticle(Long articleId, ArticleRequestDto articleRequestDto) {
        Article foundArticle = findArticleById(articleId);
        foundArticle.update(articleRequestDto);
        return new ArticleResponseDto(foundArticle);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        Article foundArticle = findArticleById(articleId);
        articleRepository.delete(foundArticle);
    }

    public Article findArticleById(Long articleId) {
        return articleRepository.findById(articleId).orElseThrow(() ->
                new IllegalArgumentException("선택한 id의 게시판은 존재하지 않습니다."));
    }
}
