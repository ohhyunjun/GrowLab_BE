package com.metaverse.growlab_be.species.dto;

import com.metaverse.growlab_be.species.domain.Species;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SpeciesResponseDto {

    private Long id;
    private String name;
    private int daysToMature;
    private String aiPromptGuideline;

    public SpeciesResponseDto(Species species) {
        this.id = species.getId();
        this.name = species.getName();
        this.daysToMature = species.getDaysToMature();
        this.aiPromptGuideline = species.getAiPromptGuideline();
    }
}