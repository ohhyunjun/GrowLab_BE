package com.metaverse.growlab_be.comment.domain;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.comment.dto.CommentRequestDto;
import com.metaverse.growlab_be.common.TimeStamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comment")
public class Comment extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    public Comment(CommentRequestDto commentRequestDto, Article article) {
        this.content = commentRequestDto.getContent();
        this.article = article;
    }

    public void update(CommentRequestDto commentRequestDto) {
        this.content = commentRequestDto.getContent();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
