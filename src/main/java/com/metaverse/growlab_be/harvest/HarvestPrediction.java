package com.metaverse.growlab_be.harvest;

import com.metaverse.growlab_be.plant.domain.Plant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name="harvest_prediction", uniqueConstraints=@UniqueConstraint(name="uk_harvest_observation",columnNames={"device_serial","observation_id"}),
       indexes=@Index(name="idx_harvest_plant_captured",columnList="plant_id,captured_at"))
@Getter @NoArgsConstructor
public class HarvestPrediction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plant_id",nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Plant plant;
    @Column(nullable=false) private String deviceSerial;
    @Column(nullable=false,length=36) private String observationId;
    @Column(nullable=false) private Long photoId;
    @Column(nullable=false) private Instant capturedAt;
    @Column(nullable=false) private Instant predictedAt;
    @Column(nullable=false) private Instant receivedAt;
    @Column(nullable=false,length=64) private String modelVersion;
    @Column(nullable=false,length=64) private String featureVersion;
    @Column(nullable=false,length=32) private String status;
    @Column(length=16) private String anchor;
    private Instant anchorAt;
    private Double remainingDays;
    private Instant expectedAt;
    @Column(columnDefinition="TEXT") private String featuresJson;

    public HarvestPrediction(Plant plant, Long photoId, HarvestPayload p, String featuresJson) {
        this.plant=plant; this.deviceSerial=plant.getDevice().getId(); this.photoId=photoId;
        observationId=p.observationId(); capturedAt=p.capturedAt(); predictedAt=p.predictedAt(); receivedAt=Instant.now();
        modelVersion=p.modelVersion(); featureVersion=p.featureVersion(); status=p.status(); anchor=p.anchor();
        anchorAt=p.anchorAt(); remainingDays=p.remainingDays(); expectedAt=p.expectedAt(); this.featuresJson=featuresJson;
    }
}
