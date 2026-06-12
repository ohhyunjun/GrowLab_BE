package com.metaverse.growlab_be.device.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceSpeciesRequestDto {
    @NotNull(message = "품종 ID는 필수입니다.")
    private Long speciesId;
}
