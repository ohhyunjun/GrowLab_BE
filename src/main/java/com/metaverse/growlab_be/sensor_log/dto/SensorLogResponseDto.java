package com.metaverse.growlab_be.sensor_log.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SensorLogResponseDto {
    private Long id;
    private String serial_number;
    private Float temperature;
    private Float humidity;
    private Float ph;
    private Float tds;
    private Boolean water_level_status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public SensorLogResponseDto(SensorLog sensorLog) {
        this.id = sensorLog.getId();

        if (sensorLog.getSerialNumber() != null) {
            this.serial_number = sensorLog.getSerialNumber().getId();
        }

        this.temperature = sensorLog.getTemperature();
        this.humidity = sensorLog.getHumidity();
        this.ph = sensorLog.getPh();
        this.tds = sensorLog.getTds();
        this.water_level_status = sensorLog.getWater_level_status();

        this.createdAt = sensorLog.getCreatedAt();

    }
}
