package com.metaverse.growlab_be.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.device.domain.Device;
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

    private Long speciesId;
    private String speciesName;

    // 이 기기에 지정된 품종의 센서 적정 범위
    private Double minTemperature;
    private Double maxTemperature;
    private Double minHumidity;
    private Double maxHumidity;
    private Double minPh;
    private Double maxPh;
    private Double minTds;
    private Double maxTds;

    // ✅ 이 기기 대표 품종의 생육 단계 정보 (프론트에서 차트/라벨 그릴 때 사용)
    private List<String> stageNames;
    private List<Integer> stageDurationDays;
    private Integer stageCount;

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
        this.serialNumber   = device.getId();
        this.deviceNickname = device.getDeviceNickname();
        this.status         = device.getStatus();
        this.ledStatus      = device.getLedStatus();
        this.photoInterval  = device.getPhotoInterval();
        this.lastPhotoAt    = lastPhotoAt;
        this.portStatus     = device.getPortStatus();
        this.createdAt      = device.getCreatedAt();
        this.updatedAt      = device.getUpdatedAt();
        this.ledMode        = device.getLedMode();
        this.ledOnTime      = device.getLedOnTime();
        this.ledOffTime     = device.getLedOffTime();
        this.plants         = plants;
        this.speciesId      = device.getSpecies() != null ? device.getSpecies().getId()   : null;
        this.speciesName    = device.getSpecies() != null ? device.getSpecies().getName() : null;
        this.minTemperature = device.getSpecies() != null ? device.getSpecies().getMinTemperature() : null;
        this.maxTemperature = device.getSpecies() != null ? device.getSpecies().getMaxTemperature() : null;
        this.minHumidity    = device.getSpecies() != null ? device.getSpecies().getMinHumidity()    : null;
        this.maxHumidity    = device.getSpecies() != null ? device.getSpecies().getMaxHumidity()    : null;
        this.minPh          = device.getSpecies() != null ? device.getSpecies().getMinPh()          : null;
        this.maxPh          = device.getSpecies() != null ? device.getSpecies().getMaxPh()          : null;
        this.minTds         = device.getSpecies() != null ? device.getSpecies().getMinTds()         : null;
        this.maxTds         = device.getSpecies() != null ? device.getSpecies().getMaxTds()         : null;
        this.stageNames     = device.getSpecies() != null ? device.getSpecies().getStageNames() : null;
        this.stageCount     = device.getSpecies() != null ? device.getSpecies().getStageCount() : null;
        this.stageDurationDays = device.getSpecies() != null ? device.getSpecies().getStageDurationDays() : null;
    }

    @Getter
    @AllArgsConstructor
    public static class PlantSummaryDto {
        private Long       id;
        private String     name;
        private Integer    portIndex;
        private String     species;

        // ✅ 생육 단계 - 인덱스 + 이 품종에서의 실제 이름
        private Integer    stageIndex;
        private String     stageName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime plantedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime germinatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime maturedAt;
    }
}
