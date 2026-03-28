package com.metaverse.growlab_be.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class DeviceCreateRequestDto {
    @NotBlank(message = "시리얼 번호는 필수 입력 값입니다.")
    private String serialNumber;

    private String deviceNickname;
}
