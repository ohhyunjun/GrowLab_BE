package com.metaverse.growlab_be.species.dto;

import com.metaverse.growlab_be.species.domain.Category;
import com.metaverse.growlab_be.species.domain.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotNull(message = "난이도는 필수입니다.")
    private Difficulty difficulty;

    private String aiPromptGuideline;
}
