package com.metaverse.growlab_be.plant.dto;

import com.metaverse.growlab_be.plant.domain.Plant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlantResponseDto {
    private Long id;
    private String name;           // 식물 이름
    private String plantStage;     // 현재 성장 단계
    private String plantedAt;      // 심은 날짜
    private String germinatedAt;   // 발아 날짜

    public PlantResponseDto(Plant plant) {
        this.id = plant.getId();
        this.name = plant.getName();
        this.plantStage = plant.getPlantStage();
        // 날짜가 null일 수 있으므로 안전하게 처리하거나 toString() 활용
        this.plantedAt = plant.getPlantedAt() != null ? plant.getPlantedAt().toString() : null;
        this.germinatedAt = plant.getGerminatedAt() != null ? plant.getGerminatedAt().toString() : null;
    }
}
