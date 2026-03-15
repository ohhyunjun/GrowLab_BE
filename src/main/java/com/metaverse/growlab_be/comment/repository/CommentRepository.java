package com.metaverse.growlab_be.comment.repository;

import com.metaverse.growlab_be.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
