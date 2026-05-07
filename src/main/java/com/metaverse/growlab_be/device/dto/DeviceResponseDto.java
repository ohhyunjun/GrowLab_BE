package com.metaverse.growlab_be.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.plant.domain.PlantStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DeviceResponseDto {
    private String serialNumber;
    private String deviceNickname;
    private Boolean status;
    private Boolean ledStatus;
    private Integer photoInterval;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPhotoAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private PlantSummaryDto plant;

    public DeviceResponseDto(Device device, LocalDateTime lastPhotoAt, PlantSummaryDto plant) {
        this.serialNumber = device.getId();
        this.deviceNickname = device.getDeviceNickname();
        this.status = device.getStatus();
        this.ledStatus = device.getLedStatus();
        this.photoInterval = device.getPhotoInterval();
        this.lastPhotoAt = lastPhotoAt;
        this.createdAt = device.getCreatedAt();
        this.updatedAt = device.getUpdatedAt();
        this.plant = plant;
    }

    // 식물 요약 정보
    @Getter
    @AllArgsConstructor
    public static class PlantSummaryDto {
        private Long id;
        private String name;
        private String species;
        private PlantStage plantStage;
    }
}
