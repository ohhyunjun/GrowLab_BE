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

        // 최근 관측된 질병 결과 (없으면 null). 포트 헬스 표시/문제 분석에 사용.
        private String     diseaseResult;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime plantedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime germinatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime maturedAt;

        private Long speciesId;
        private Double minTemperature;
        private Double maxTemperature;
        private Double minHumidity;
        private Double maxHumidity;
        private Double minPh;
        private Double maxPh;
        private Double minTds;
        private Double maxTds;
        private List<String> stageNames;
        private List<Integer> stageDurationDays;
        private Integer stageCount;
    }
}
