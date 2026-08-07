package com.metaverse.growlab_be.plant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PlantResponseDto {

    private Long id;
    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plantedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime germinatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maturedAt;

    private Long speciesId;
    private String speciesName;
    private Integer daysToMature;

    // ✅ 생육 단계 - 인덱스 + 이 품종에서의 실제 이름 + 총 단계 수
    private Integer stageIndex;
    private String stageName;
    private Integer stageCount;

    private String deviceSerial;
    private String deviceNickname;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Integer portIndex;

    private String diseaseResult;

    private List<DiaryResponseDto> diaries;

    public PlantResponseDto(Plant plant) {
        this.id = plant.getId();
        this.name = plant.getName();

        this.plantedAt = plant.getPlantedAt();
        this.germinatedAt = plant.getGerminatedAt();
        this.maturedAt = plant.getMaturedAt();
        this.createdAt = plant.getCreatedAt();
        this.updatedAt = plant.getUpdatedAt();

        this.stageIndex = plant.getStageIndex();

        if (plant.getDevice() != null) {
            this.deviceSerial = plant.getDevice().getId();
            this.deviceNickname = plant.getDevice().getDeviceNickname();
        }

        this.portIndex = plant.getPortIndex();
        this.diseaseResult = plant.getDiseaseResult();

        if (plant.getDiaries() != null) {
            this.diaries = plant.getDiaries()
                    .stream()
                    .map(DiaryResponseDto::new)
                    .collect(Collectors.toList());
        }

        if (plant.getDevice() != null && plant.getDevice().getSpecies() != null) {
            Species species = plant.getDevice().getSpecies();
            this.speciesId = species.getId();
            this.speciesName = species.getName();
            this.daysToMature = species.getDaysToMature();
            this.stageName = species.getStageName(plant.getStageIndex());
            this.stageCount = species.getStageCount();
        }
    }
}