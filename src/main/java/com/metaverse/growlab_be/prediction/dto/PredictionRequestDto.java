package com.metaverse.growlab_be.prediction.dto;

import lombok.Getter;

@Getter
public class PredictionRequestDto {
    private Long    plant_id;
    private Integer predicted_stage;
    private Float   confidence;
    private String  prediction_mode;
    private Integer eta_lower_hours;
    private Integer eta_upper_hours;
    private Integer eta_point_hours;
}
