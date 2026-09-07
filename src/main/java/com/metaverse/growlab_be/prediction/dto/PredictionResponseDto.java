package com.metaverse.growlab_be.prediction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PredictionResponseDto {

    private Long id;
    private Long plantId;
    private Integer predictedStage;
    private String predictedStageName;
    private Float confidence;
    private String predictionMode;
    private Integer etaLowerHours;
    private Integer etaPointHours;
    private Integer etaUpperHours;
    private Long correctionHours;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime baselineAt;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime expectedAt;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime rangeStartAt;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime rangeEndAt;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime predictedAt;

    public PredictionResponseDto(Prediction prediction, String predictedStageName,
                                 Long correctionHours, LocalDateTime baselineAt, LocalDateTime expectedAt,
                                 LocalDateTime rangeStartAt, LocalDateTime rangeEndAt) {
        this.id = prediction.getId();
        this.plantId = prediction.getPlant().getId();
        this.predictedStage = prediction.getPredictedStage();
        this.predictedStageName = predictedStageName;
        this.confidence = prediction.getConfidence();
        this.predictionMode = prediction.getPredictionMode();
        this.etaLowerHours = prediction.getEtaLowerHours();
        this.etaPointHours = prediction.getEtaPointHours();
        this.etaUpperHours = prediction.getEtaUpperHours();
        this.correctionHours = correctionHours;
        this.baselineAt = baselineAt;
        this.expectedAt = expectedAt;
        this.rangeStartAt = rangeStartAt;
        this.rangeEndAt = rangeEndAt;
        this.predictedAt = prediction.getCreatedAt();
    }
}
