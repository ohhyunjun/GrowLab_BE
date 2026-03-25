package com.metaverse.growlab_be.plant.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.species.domain.Species;
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
@Table(name = "plant")
public class Plant extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 식물 이름

    @Column(nullable = false)
    private LocalDateTime plantedAt; // 심은 날짜

    @Column
    private LocalDateTime germinatedAt; // 발아 날짜

    @Column(nullable = false)
    private String plantStage; // 성장 단계

    // Diary와의 1:N 관계
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Diary> diaries = new ArrayList<>();

    public Plant(PlantRequestDto plantRequestDto) {
        this.name = plantRequestDto.getName();
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.plantStage = plantRequestDto.getPlantStage();
    }

    public void update(PlantRequestDto plantRequestDtorequestDto) {
        this.name = plantRequestDtorequestDto.getName();
        this.plantStage = plantRequestDtorequestDto.getPlantStage();
        this.germinatedAt = plantRequestDtorequestDto.getGerminatedAt();
    }
}
