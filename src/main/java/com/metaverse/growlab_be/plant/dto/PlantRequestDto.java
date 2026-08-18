package com.metaverse.growlab_be.plant.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PlantRequestDto {
    private String name;
    private Integer stageIndex;      // ✅ 몇 번째 단계인지 (0부터 시작)
    private LocalDateTime plantedAt;
    private LocalDateTime germinatedAt;
    private LocalDateTime maturedAt;
    private String serialNumber;
    private Integer portIndex;
}
