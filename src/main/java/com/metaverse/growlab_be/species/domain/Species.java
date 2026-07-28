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

    // AI 분석을 위한 보조 설명(생육 특이사항 등, 선택 입력)
    @Column(length = 500)
    private String aiPromptGuideline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    // ✅ 재배 기준 (전부 선택 입력, 미입력 시 AI 조언에서 "기준 미등록"으로 처리)
    @Column(name = "min_temperature")
    private Double minTemperature;

    @Column(name = "max_temperature")
    private Double maxTemperature;

    @Column(name = "min_humidity")
    private Double minHumidity;

    @Column(name = "max_humidity")
    private Double maxHumidity;

    @Column(name = "min_ph")
    private Double minPh;

    @Column(name = "max_ph")
    private Double maxPh;

    @Column(name = "min_tds")
    private Double minTds;

    @Column(name = "max_tds")
    private Double maxTds;

    @Column(name = "min_light_hours")
    private Double minLightHours;

    @Column(name = "max_light_hours")
    private Double maxLightHours;

    // Plant와 1:N 관계
    @OneToMany(mappedBy = "species", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plant> plants = new ArrayList<>();

    public Species(SpeciesRequestDto speciesRequestDto) {
        applyFrom(speciesRequestDto);
    }

    public void update(SpeciesRequestDto speciesRequestDto) {
        applyFrom(speciesRequestDto);
    }

    private void applyFrom(SpeciesRequestDto dto) {
        this.name = dto.getName();
        this.daysToMature = dto.getDaysToMature();
        this.aiPromptGuideline = dto.getAiPromptGuideline();
        this.category = dto.getCategory();
        this.difficulty = dto.getDifficulty();
        this.minTemperature = dto.getMinTemperature();
        this.maxTemperature = dto.getMaxTemperature();
        this.minHumidity = dto.getMinHumidity();
        this.maxHumidity = dto.getMaxHumidity();
        this.minPh = dto.getMinPh();
        this.maxPh = dto.getMaxPh();
        this.minTds = dto.getMinTds();
        this.maxTds = dto.getMaxTds();
        this.minLightHours = dto.getMinLightHours();
        this.maxLightHours = dto.getMaxLightHours();
    }
}