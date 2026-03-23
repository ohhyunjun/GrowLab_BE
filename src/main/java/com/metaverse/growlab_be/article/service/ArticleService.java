package com.metaverse.growlab_be.article.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.metaverse.growlab_be.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;

    @Transactional
    public ArticleResponseDto createArticle(ArticleRequestDto articleRequestDto, PrincipalDetails principalDetails) {
        User logginedUser = principalDetails.user();
        Article newArticle = new Article(articleRequestDto, logginedUser);
        Article savedArticle = articleRepository.save(newArticle);
        ArticleResponseDto articleResponseDto = new ArticleResponseDto(savedArticle);
        return articleResponseDto;
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponseDto> getArticles(Pageable pageable) {
        Page<ArticleResponseDto> articleResponseDtoPaginationList = articleRepository.findAllByOrderByCreatedAtDesc(pageable).map(ArticleResponseDto::new);
        return articleResponseDtoPaginationList;
    }

    @Transactional(readOnly = true)
    public ArticleResponseDto getArticleById(Long articleId) {
        Article foundArticle = getValidArticleById(articleId);
        ArticleResponseDto articleResponseDto = new ArticleResponseDto(foundArticle);
        return articleResponseDto;
    }

    @Transactional
    public ArticleResponseDto updateArticle(Long articleId, ArticleRequestDto articleRequestDto) {
        Article foundArticle = getValidArticleById(articleId);
        foundArticle.update(articleRequestDto);
        return new ArticleResponseDto(foundArticle);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        Article foundArticle = getValidArticleById(articleId);
        articleRepository.delete(foundArticle);
    }

    //좋아요 시스템이 구현이 안되어 있어서 일단 놔둠
    public List<ArticleResponseDto> getLikedArticles() {
        return null;
    }

    public void toggleArticleLike(Long articleId) {
        return;
    }

    public Article getValidArticleById(Long articleId) {
        return articleRepository.findById(articleId).orElseThrow(() ->
                new IllegalArgumentException("선택한 id의 게시글은 존재하지 않습니다."));
    }
}
