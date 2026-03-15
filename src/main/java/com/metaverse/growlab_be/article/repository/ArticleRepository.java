package com.metaverse.growlab_be.article.repository;

import com.metaverse.growlab_be.article.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
