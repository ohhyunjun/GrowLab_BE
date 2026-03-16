package com.metaverse.growlab_be.plant.dto;

import com.metaverse.growlab_be.plant.domain.Plant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class PlantResponseDto {
    private Long id;
    private String name;           // 식물 이름
    private String plantStage;     // 현재 성장 단계
    private LocalDate plantedAt;      // 심은 날짜
    private LocalDate germinatedAt;   // 발아 날짜

    public PlantResponseDto(Plant plant) {
        this.id = plant.getId();
        this.name = plant.getName();
        this.plantStage = plant.getPlantStage();
        this.plantedAt = plant.getPlantedAt();
        this.germinatedAt = plant.getGerminatedAt();
    }
}
