package com.metaverse.growlab_be.species.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SpeciesRequestDto {

    @NotBlank(message = "품종 이름은 필수 입력 값입니다.")
    private String name;

    @Positive(message = "성숙 기간은 양수여야 합니다.")
    private int daysToMature;

    private String aiPromptGuideline;
}
