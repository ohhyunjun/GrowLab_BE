package com.metaverse.growlab_be.species.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.dto.SpeciesRequestDto;
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
@Table(name = "species")
public class Species extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 종 이름 (예: "방울토마토", "적상추")
    @Column(nullable = false, unique = true)
    private String name;

    // 발아 후 성체까지 걸리는 평균 일수
    @Column(nullable = false)
    private int daysToMature;

    // AI 분석을 위한 가이드라인 텍스트
    @Column(length = 500)
    private String aiPromptGuideline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    // Plant와 1:N 관계
    @OneToMany(mappedBy = "species", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plant> plants = new ArrayList<>();

    public Species(SpeciesRequestDto speciesRequestDto) {
        this.name = speciesRequestDto.getName();
        this.daysToMature = speciesRequestDto.getDaysToMature();
        this.aiPromptGuideline = speciesRequestDto.getAiPromptGuideline();
        this.category = speciesRequestDto.getCategory();
        this.difficulty = speciesRequestDto.getDifficulty();
    }

    public void update(SpeciesRequestDto speciesRequestDto) {
        this.name = speciesRequestDto.getName();
        this.daysToMature = speciesRequestDto.getDaysToMature();
        this.aiPromptGuideline = speciesRequestDto.getAiPromptGuideline();
    }
}
