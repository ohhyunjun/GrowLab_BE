package com.metaverse.growlab_be.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import java.time.LocalTime;

@Getter
public class LedRequestDto {
    private Boolean ledStatus;

    private Boolean ledMode;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime ledOnTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime ledOffTime;
}
