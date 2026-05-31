package com.metaverse.growlab_be.prediction.dto;

import lombok.Getter;

@Getter
public class PredictionRequestDto {
    private Long    plant_id;
    private Integer predicted_stage;
    private Float   confidence;
    private Integer germination_eta_hours;
    private Integer mature_eta_hours;
}
