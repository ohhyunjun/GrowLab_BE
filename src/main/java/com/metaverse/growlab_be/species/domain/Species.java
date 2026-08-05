package com.metaverse.growlab_be.species.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
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

    // 기본 단계명 (관리자가 별도로 지정하지 않은 품종에 사용, 기존 데이터 호환용)
    private static final List<String> DEFAULT_STAGE_NAMES = List.of("씨앗", "발아", "수확");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int daysToMature;

    @Column(length = 500)
    private String aiPromptGuideline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

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

    // ✅ 이 품종의 생육 단계 이름 목록 (순서 있음, 개수 가변)
    // 예: 상추 -> ["씨앗", "발아", "수확"], 딸기 -> ["발아", "육묘", "개화", "착과", "수확"]
    @ElementCollection
    @CollectionTable(name = "species_stage", joinColumns = @JoinColumn(name = "species_id"))
    @OrderColumn(name = "stage_order")
    @Column(name = "stage_name", nullable = false)
    private List<String> stageNames = new ArrayList<>();

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

        // 단계명이 요청에 명시되어 있으면 교체, 없으면 기존 값 유지(없으면 기본값)
        if (dto.getStageNames() != null && !dto.getStageNames().isEmpty()) {
            this.stageNames = new ArrayList<>(dto.getStageNames());
        } else if (this.stageNames == null || this.stageNames.isEmpty()) {
            this.stageNames = new ArrayList<>(DEFAULT_STAGE_NAMES);
        }
    }

    // 이 품종의 총 단계 개수
    public int getStageCount() {
        return stageNames == null || stageNames.isEmpty() ? DEFAULT_STAGE_NAMES.size() : stageNames.size();
    }

    // index 번째 단계 이름 (범위를 벗어나면 마지막/처음 단계로 안전 처리)
    public String getStageName(int index) {
        List<String> names = (stageNames == null || stageNames.isEmpty()) ? DEFAULT_STAGE_NAMES : stageNames;
        int safeIndex = Math.max(0, Math.min(index, names.size() - 1));
        return names.get(safeIndex);
    }
}