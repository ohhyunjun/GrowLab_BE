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

    // LightGBM: 72h 후 예측 단계 (0=SEED, 1=GERMINATION, 2=MATURE)
    @Column(name = "predicted_stage", nullable = false)
    private Integer predictedStage;

    // LightGBM: 신뢰도
    @Column(name = "confidence", nullable = false)
    private Float confidence;

    // DeepHit 0→1: 씨앗→발아 예상 시간(시간), SEED 단계일 때만 유효
    @Column(name = "germination_eta_hours")
    private Integer germinationEtaHours;

    // DeepHit 1→2: 발아→수확 예상 시간(시간)
    @Column(name = "mature_eta_hours")
    private Integer matureEtaHours;

    public Prediction(Plant plant, Integer predictedStage, Float confidence,
                      Integer germinationEtaHours, Integer matureEtaHours) {
        this.plant = plant;
        this.predictedStage = predictedStage;
        this.confidence = confidence;
        this.germinationEtaHours = germinationEtaHours;
        this.matureEtaHours = matureEtaHours;
    }


}
