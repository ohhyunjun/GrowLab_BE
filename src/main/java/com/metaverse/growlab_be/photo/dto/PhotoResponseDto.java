package com.metaverse.growlab_be.photo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.metaverse.growlab_be.photo.domain.Photo;
import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class PhotoResponseDto {

    private final Long id;
    private final String filePath;
    private final String fileName;
    private final String deviceSerialNumber;

    private final String growthResult;
    private final Double confidence;
    private final String diseaseResult;
    private final Double diseaseConfidence;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    // Photo 엔티티를 DTO로 변환하는 public 생성자
    public PhotoResponseDto(Photo photo) {
        this.id = photo.getId();
        this.filePath = photo.getFilePath();
        this.fileName = photo.getFileName();
        // Device가 null인 경우를 대비한 안전한 처리
        this.deviceSerialNumber = photo.getDevice() != null ? photo.getDevice().getId() : null;
        this.createdAt = photo.getCreatedAt();
        this.growthResult = photo.getGrowthStageResult();
        this.confidence = photo.getConfidence();
        this.diseaseResult = photo.getDiseaseResult();
        this.diseaseConfidence = photo.getDiseaseConfidence();
    }
}