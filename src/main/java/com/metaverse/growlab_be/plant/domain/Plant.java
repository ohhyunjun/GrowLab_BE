package com.metaverse.growlab_be.plant.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.prediction.domain.Prediction;
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
                columnNames = {"device_serial", "port_index"}
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

    // ✅ 몇 번째 생육 단계인지 (0부터 시작). 실제 이름은 Device.species.getStageName(stageIndex)로 조회
    @Column(nullable = false, name = "stage_index")
    private Integer stageIndex = 0;

    @Column(nullable = false, name = "port_index")
    private Integer portIndex;

    @Column
    private String diseaseResult;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions = new ArrayList<>();

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Diary> diaries = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_serial", nullable = false)
    private Device device;

    public Plant(PlantRequestDto plantRequestDto, Device device) {
        this.name = plantRequestDto.getName();
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDto.getMaturedAt();
        this.stageIndex = plantRequestDto.getStageIndex() != null ? plantRequestDto.getStageIndex() : 0;
        this.portIndex = plantRequestDto.getPortIndex();
        this.device = device;
    }

    public void update(PlantRequestDto plantRequestDto) {
        this.name = plantRequestDto.getName();
        this.stageIndex = plantRequestDto.getStageIndex() != null ? plantRequestDto.getStageIndex() : 0;
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDto.getMaturedAt();
    }
}
