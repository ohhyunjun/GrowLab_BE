package com.metaverse.growlab_be.plant.dto;

import com.metaverse.growlab_be.plant.domain.PlantStage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PlantRequestDto {
    private String name;           // 식물 이름
    private PlantStage plantStage;     // 현재 성장 단계
    private LocalDateTime plantedAt; // 심은 날짜
    private LocalDateTime germinatedAt; // 발아 날짜
    private LocalDateTime maturedAt; // 성숙 날짜
    private Long speciesId; // 종 이름
    private String deviceSerial; // 시리얼 넘버
}
