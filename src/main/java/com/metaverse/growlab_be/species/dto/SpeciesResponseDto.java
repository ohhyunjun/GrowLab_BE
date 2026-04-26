package com.metaverse.growlab_be.species.dto;

import com.metaverse.growlab_be.species.domain.Category;
import com.metaverse.growlab_be.species.domain.Difficulty;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class SpeciesResponseDto {

    private Long id;
    private String name;
    private int daysToMature;
    private String aiPromptGuideline;
    private Category category;
    private Difficulty difficulty;

    public SpeciesResponseDto(Species species) {
        this.id = species.getId();
        this.name = species.getName();
        this.daysToMature = species.getDaysToMature();
        this.aiPromptGuideline = species.getAiPromptGuideline();
        this.category = species.getCategory();
        this.difficulty = species.getDifficulty();
    }
}