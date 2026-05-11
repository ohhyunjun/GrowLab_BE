package com.metaverse.growlab_be.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class PhotoIntervalRequestDto {
    @Min(value = 1, message = "올바른 촬영 주기를 선택해주세요.")
    @Max(value = 24, message = "올바른 촬영 주기를 선택해주세요.")
    private Integer photoInterval;

}
