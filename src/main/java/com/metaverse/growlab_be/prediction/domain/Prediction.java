package com.metaverse.growlab_be.prediction.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.plant.domain.Plant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prediction")
public class Prediction extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "predicted_stage", nullable = false)
    private Integer predictedStage;

    // q10~q90 예측 범위의 목표 신뢰수준 (현재 0.8)
    @Column(name = "confidence", nullable = false)
    private Float confidence;

    // AI 보정 결과인지, 센서 이력 수집 중의 통계 예상인지 구분한다.
    @Column(name = "prediction_mode")
    private String predictionMode;

    // 예측 단계에 빠르게 도달할 경우의 남은 시간
    @Column(name = "eta_lower_hours")
    private Integer etaLowerHours;

    // 예측 단계에 늦게 도달할 경우의 남은 시간
    @Column(name = "eta_upper_hours")
    private Integer etaUpperHours;

    // 예측 단계에 도달할 가능성이 가장 높은 남은 시간
    @Column(name = "eta_point_hours")
    private Integer etaPointHours;


    public Prediction(Plant plant, Integer predictedStage, Float confidence,
                      Integer etaLowerHours, Integer etaPointHours, Integer etaUpperHours) {
        this.plant = plant;
        this.predictedStage = predictedStage;
        this.confidence = confidence;
        this.etaLowerHours = etaLowerHours;
        this.etaPointHours = etaPointHours;
        this.etaUpperHours = etaUpperHours;
    }


}
