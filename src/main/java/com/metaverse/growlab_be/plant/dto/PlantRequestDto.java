package com.metaverse.growlab_be.plant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class PlantRequestDto {
    private String name;           // 식물 이름
    private String plantStage;     // 현재 성장 단계
    private LocalDateTime plantedAt; // 심은 날짜
    private LocalDateTime germinatedAt; // 발아 날짜
    private Long speciesId;
}
