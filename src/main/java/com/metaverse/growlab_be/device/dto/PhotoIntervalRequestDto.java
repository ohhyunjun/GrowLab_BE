package com.metaverse.growlab_be.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PhotoIntervalRequestDto {
    @NotNull(message = "촬영 주기는 필수입니다.")
    @Min(value = 1, message = "올바른 촬영 주기를 선택해주세요.")
    @Max(value = 6, message = "촬영 주기는 최대 6시간까지 설정할 수 있습니다.")
    private Integer photoInterval;

}
