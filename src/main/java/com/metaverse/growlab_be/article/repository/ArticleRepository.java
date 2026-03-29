package com.metaverse.growlab_be.article.repository;

import com.metaverse.growlab_be.article.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findAllByOrderByCreatedAtDesc();

    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);

    //Page<Article> findAllByLikesUserId(Long userId, Pageable pageable);
}
