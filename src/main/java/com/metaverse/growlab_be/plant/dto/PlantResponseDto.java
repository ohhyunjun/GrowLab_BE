package com.metaverse.growlab_be.plant.dto;

import com.metaverse.growlab_be.plant.domain.Plant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PlantResponseDto {
    private Long id;
    private String name;           // 식물 이름
    private String plantStage;     // 현재 성장 단계
    private LocalDateTime plantedAt;      // 심은 날짜
    private LocalDateTime germinatedAt;   // 발아 날짜
    private Long speciesId;       // 종 정보 (ID)
    private String speciesName;   // 종 정보 (이름)

    public PlantResponseDto(Plant plant) {
        this.id = plant.getId();
        this.name = plant.getName();
        this.plantStage = plant.getPlantStage();
        this.plantedAt = plant.getPlantedAt();
        this.germinatedAt = plant.getGerminatedAt();


        // Species 매핑
        if (plant.getSpecies() != null) {
            this.speciesId = plant.getSpecies().getId();
            this.speciesName = plant.getSpecies().getName();
        }
    }
}
