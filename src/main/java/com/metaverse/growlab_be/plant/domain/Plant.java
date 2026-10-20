package com.metaverse.growlab_be.plant.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import com.metaverse.growlab_be.species.domain.Species;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plant", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_device_port",
                columnNames = {"device_serial", "active_port_index"}
        )
})
public class Plant extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime plantedAt;

    @Column
    private LocalDateTime germinatedAt;

    @Column
    private LocalDateTime maturedAt;

    // 몇 번째 생육 단계인지 (0부터 시작). 실제 이름은 이 식물의 품종에서 조회한다.
    @Column(nullable = false, name = "stage_index")
    private Integer stageIndex = 0;

    @Column(nullable = false, name = "port_index")
    private Integer portIndex;

    @Column(name = "active_port_index")
    private Integer activePortIndex;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason")
    private PlantEndReason endReason;

    @Column
    private String diseaseResult;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_serial", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false, foreignKey = @ForeignKey(name = "fk_plant_species"))
    private Species species;

    public Plant(PlantRequestDto plantRequestDto, Device device) {
        this.name = plantRequestDto.getName();
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDto.getMaturedAt();
        this.stageIndex = plantRequestDto.getStageIndex() != null ? plantRequestDto.getStageIndex() : 0;
        this.portIndex = plantRequestDto.getPortIndex();
        this.activePortIndex = plantRequestDto.getPortIndex();
        this.device = device;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public void finish(PlantEndReason reason) {
        if (reason == null) throw new IllegalArgumentException("재배 종료 사유가 필요합니다.");
        if (!isActive()) throw new IllegalStateException("이미 종료된 식물입니다.");
        this.endedAt = LocalDateTime.now();
        this.endReason = reason;
        this.activePortIndex = null;
    }

    public void update(PlantRequestDto plantRequestDto) {
        this.name = plantRequestDto.getName();
        this.stageIndex = plantRequestDto.getStageIndex() != null ? plantRequestDto.getStageIndex() : 0;
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDto.getMaturedAt();
    }
}
