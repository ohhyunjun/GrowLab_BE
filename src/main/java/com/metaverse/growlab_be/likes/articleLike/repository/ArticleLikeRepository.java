package com.metaverse.growlab_be.likes.articleLike.repository;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.likes.articleLike.domain.ArticleLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    Optional<ArticleLike> findByArticleAndUser(Article article, User user);

    long countByArticle(Article article);

    Page<ArticleLike> findByUser(User user, Pageable pageable);
}