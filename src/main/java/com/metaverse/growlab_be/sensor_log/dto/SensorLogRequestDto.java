package com.metaverse.growlab_be.sensor_log.dto;

import lombok.Getter;

@Getter
public class SensorLogRequestDto {
    private String serial_number;

    private Float temperature;
    private Float humidity;
    private Float ph;
    private Float tds;
    private Boolean water_level_status;
}
