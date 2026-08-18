package com.metaverse.growlab_be.species.dto;

import com.metaverse.growlab_be.species.domain.Category;
import com.metaverse.growlab_be.species.domain.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SpeciesRequestDto {

    @NotBlank(message = "품종 이름은 필수 입력 값입니다.")
    private String name;

    @Positive(message = "성숙 기간은 양수여야 합니다.")
    private int daysToMature;

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotNull(message = "난이도는 필수입니다.")
    private Difficulty difficulty;

    private String aiPromptGuideline;

    private Double minTemperature;
    private Double maxTemperature;
    private Double minHumidity;
    private Double maxHumidity;
    private Double minPh;
    private Double maxPh;
    private Double minTds;
    private Double maxTds;
    private Double minLightHours;
    private Double maxLightHours;

    private List<String> stageNames;

    // ✅ 각 단계 시작일 (stageNames와 같은 개수/순서로 맞춰서 보내야 함)
    private List<Integer> stageDurationDays;
}