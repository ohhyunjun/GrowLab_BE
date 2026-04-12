package com.metaverse.growlab_be.article.domain;

import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.comment.domain.Comment;
import com.metaverse.growlab_be.common.TimeStamped;
import com.metaverse.growlab_be.file.domain.File;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "article")
public class Article extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Comment와의 1:N 관계 설정
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Comment> comments = new ArrayList<>();

    // File와의 1:N 관계 설정
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    List<File> files = new ArrayList<>();

    // User와의 N:1 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Article(ArticleRequestDto articleRequestDto, User user) {
        this.title = articleRequestDto.getTitle();
        this.content = articleRequestDto.getContent();
        this.user = user;
    }

    public void update(ArticleRequestDto articleRequestDto) {
        this.title = articleRequestDto.getTitle();
        this.content = articleRequestDto.getContent();
    }
}
