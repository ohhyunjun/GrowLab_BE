package com.metaverse.growlab_be.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.plant.domain.PlantStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DeviceResponseDto {
    private String serialNumber;
    private String deviceNickname;
    private Boolean status;
    private Boolean ledStatus;
    private Integer photoInterval;
    private Boolean ledMode;
    private String portStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPhotoAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime ledOnTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime ledOffTime;

    private List<PlantSummaryDto> plants;

    public DeviceResponseDto(Device device, LocalDateTime lastPhotoAt, List<PlantSummaryDto> plants) {
        this.serialNumber = device.getId();
        this.deviceNickname = device.getDeviceNickname();
        this.status = device.getStatus();
        this.ledStatus = device.getLedStatus();
        this.photoInterval = device.getPhotoInterval();
        this.lastPhotoAt = lastPhotoAt;
        this.portStatus = device.getPortStatus();
        this.createdAt = device.getCreatedAt();
        this.updatedAt = device.getUpdatedAt();
        this.ledMode = device.getLedMode();
        this.ledOnTime = device.getLedOnTime();
        this.ledOffTime = device.getLedOffTime();
        this.plants = plants;
    }

    // 식물 요약 정보
    @Getter
    @AllArgsConstructor
    public static class PlantSummaryDto {
        private Long       id;
        private String     name;
        private Integer    portIndex;
        private String     species;
        private PlantStage plantStage;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime plantedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime germinatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime maturedAt;
    }
}
