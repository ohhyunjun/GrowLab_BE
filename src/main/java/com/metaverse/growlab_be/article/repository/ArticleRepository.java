package com.metaverse.growlab_be.article.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findAllByOrderByCreatedAtDesc();

    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);

    //Page<Article> findAllByLikesUserId(Long userId, Pageable pageable);

    Page<Article> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
