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
    private Boolean led_status;
    private Integer led_on_minutes_1h;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public SensorLogResponseDto(SensorLog sensorLog) {
        this.id = sensorLog.getId();

        if (sensorLog.getDevice() != null) {
            this.serial_number = sensorLog.getDevice().getId();
        }

        this.temperature = sensorLog.getTemperature();
        this.humidity = sensorLog.getHumidity();
        this.ph = sensorLog.getPh();
        this.tds = sensorLog.getTds();
        this.water_level_status = sensorLog.getWater_level_status();
        this.led_status = sensorLog.getLed_status();
        this.led_on_minutes_1h = sensorLog.getLed_on_minutes_1h();

        this.createdAt = sensorLog.getCreatedAt();

    }
}
