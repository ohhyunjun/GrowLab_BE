package com.metaverse.growlab_be.comment.repository;

import com.metaverse.growlab_be.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

//timestamped랑 user가 없어서 경고 나 있음
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByOrderByCreatedAtDesc();

    List<Comment> findAllByArticleIdOrderByCreatedAtDesc(Long articleId);

    List<Comment> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Comment> findByIdAndArticleId(Long commentId, Long articleId);
}
