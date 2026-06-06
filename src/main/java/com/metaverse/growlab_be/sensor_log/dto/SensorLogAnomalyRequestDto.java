package com.metaverse.growlab_be.sensor_log.dto;

import lombok.Getter;

@Getter
public class SensorLogAnomalyRequestDto {
    private String serial_number;
    private String sensor_type;
    private Float value;
}
