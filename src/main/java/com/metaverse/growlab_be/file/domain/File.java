package com.metaverse.growlab_be.file.domain;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.common.TimeStamped;
import com.metaverse.growlab_be.diary.domain.Diary;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "file")
public class File extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String filePath;

    // Article와의 N:1 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = true)
    private Article article;

    public File(String originalFileName, String storedFileName, String filePath, Article article) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.article = article;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = true)
    private Diary diary;

    public File(String originalFileName, String storedFileName, String filePath, Diary diary) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.diary = diary;
    }

}
