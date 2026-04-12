package com.metaverse.growlab_be.plant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PlantResponseDto {

    private Long id;
    private String name;           // 식물 이름

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plantedAt;        // 심은 날짜

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime germinatedAt;     // 발아 날짜

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime MaturedAt;     // 발아 날짜

    private Long speciesId;       // 종 정보 (ID)
    private String speciesName;   // 종 정보 (이름)
    private Integer daysToMature;

    private PlantStage plantStage;     // 현재 성장 단계

    private String deviceSerial;
    private String deviceNickname;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 다이어리만 포함
    private List<DiaryResponseDto> diaries;

    public PlantResponseDto(Plant plant) {
        this.id = plant.getId();
        this.name = plant.getName();

        this.plantStage = plant.getPlantStage();
        this.plantedAt = plant.getPlantedAt();
        this.germinatedAt = plant.getGerminatedAt();
        this.MaturedAt = plant.getMaturedAt();
        this.createdAt = plant.getCreatedAt();
        this.updatedAt = plant.getUpdatedAt();

        this.deviceSerial = plant.getDevice().getId();
        this.deviceNickname = plant.getDevice().getDeviceNickname();

        this.diaries = plant.getDiaries()
                .stream()
                .map(DiaryResponseDto::new)
                .collect(Collectors.toList());

        // Species 매핑
        if (plant.getSpecies() != null) {
            this.speciesId = plant.getSpecies().getId();
            this.speciesName = plant.getSpecies().getName();
            this.daysToMature = plant.getSpecies().getDaysToMature();
        }
    }
}
