package com.metaverse.growlab_be.plant.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PlantRequestDto {
    private String name;           // 식물 이름
    private String plantStage;     // 현재 성장 단계
    private LocalDateTime plantedAt; // 심은 날짜
}
