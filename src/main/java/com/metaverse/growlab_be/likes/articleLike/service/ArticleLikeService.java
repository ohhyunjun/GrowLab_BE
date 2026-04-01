package com.metaverse.growlab_be.likes.articleLike.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.service.ArticleService;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.likes.articleLike.domain.ArticleLike;
import com.metaverse.growlab_be.likes.articleLike.dto.ArticleLikeResponseDto;
import com.metaverse.growlab_be.likes.articleLike.repository.ArticleLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService {
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleService articleService;

    @Transactional
    public ArticleLikeResponseDto toggleArticleLike(PrincipalDetails principalDetails, Long articleId) {
        Article article = articleService.getValidArticleById(articleId);

        User logginedUser = principalDetails.user();

        Optional<ArticleLike> existingLike = articleLikeRepository.findByArticleAndUser(article, logginedUser);

        boolean liked;
        if (existingLike.isPresent()) {
            articleLikeRepository.delete(existingLike.get());
            liked = false;
        } else {
            ArticleLike newLike = new ArticleLike(article, logginedUser);
            articleLikeRepository.save(newLike);
            liked = true;
        }

        int currentLikesCount = (int) articleLikeRepository.countByArticle(article);

        return new ArticleLikeResponseDto(article.getId(), logginedUser.getId(), liked, currentLikesCount);
    }
}
