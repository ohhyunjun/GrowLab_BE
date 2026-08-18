package com.metaverse.growlab_be.species.dto;

import com.metaverse.growlab_be.species.domain.Category;
import com.metaverse.growlab_be.species.domain.Difficulty;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SpeciesResponseDto {

    private Long id;
    private String name;
    private int daysToMature;
    private String aiPromptGuideline;
    private Category category;
    private Difficulty difficulty;

    private Double minTemperature;
    private Double maxTemperature;
    private Double minHumidity;
    private Double maxHumidity;
    private Double minPh;
    private Double maxPh;
    private Double minTds;
    private Double maxTds;
    private Double minLightHours;
    private Double maxLightHours;

    private List<String> stageNames;
    private int stageCount;
    private List<Integer> stageDurationDays; // ✅ 추가

    public SpeciesResponseDto(Species species) {
        this.id = species.getId();
        this.name = species.getName();
        this.daysToMature = species.getDaysToMature();
        this.aiPromptGuideline = species.getAiPromptGuideline();
        this.category = species.getCategory();
        this.difficulty = species.getDifficulty();
        this.minTemperature = species.getMinTemperature();
        this.maxTemperature = species.getMaxTemperature();
        this.minHumidity = species.getMinHumidity();
        this.maxHumidity = species.getMaxHumidity();
        this.minPh = species.getMinPh();
        this.maxPh = species.getMaxPh();
        this.minTds = species.getMinTds();
        this.maxTds = species.getMaxTds();
        this.minLightHours = species.getMinLightHours();
        this.maxLightHours = species.getMaxLightHours();
        this.stageNames = species.getStageNames();
        this.stageCount = species.getStageCount();
        this.stageDurationDays = species.getStageDurationDays();
    }
}