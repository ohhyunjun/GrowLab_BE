package com.metaverse.growlab_be.diary.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.image.domain.Image;
import com.metaverse.growlab_be.plant.domain.Plant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "diary")
public class Diary extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime targetDate; // Diary 작성 기준 날짜

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    // Plant과의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    // User와의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Diary(DiaryRequestDto diaryRequestDto, Plant plant, User user) {
        this.title = diaryRequestDto.getTitle();
        this.content = diaryRequestDto.getContent();
        this.targetDate = diaryRequestDto.getTargetDate();
        this.plant = plant;
        this.user = user;
    }

    public void addImage(Image image) {
        image.setDiary(this);
        this.images.add(image);
    }

    public void update(DiaryRequestDto diaryRequestDto) {
        this.title = diaryRequestDto.getTitle();
        this.content = diaryRequestDto.getContent();
        this.targetDate = diaryRequestDto.getTargetDate();
    }
}
