package com.metaverse.growlab_be.prediction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PredictionResponseDto {
    private Long    id;
    private Integer predictedStage;
    private Float   confidence;
    private Integer germinationEtaHours;
    private Integer matureEtaHours;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime predictedAt;

    public PredictionResponseDto(Prediction prediction) {
        this.id                  = prediction.getId();
        this.predictedStage      = prediction.getPredictedStage();
        this.confidence          = prediction.getConfidence();
        this.germinationEtaHours = prediction.getGerminationEtaHours();
        this.matureEtaHours      = prediction.getMatureEtaHours();
        this.predictedAt         = prediction.getCreatedAt();
    }
}
