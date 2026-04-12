package com.metaverse.growlab_be.plant.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
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
@Table(name = "plant")
public class Plant extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 식물 이름

    @Column(nullable = false)
    private LocalDateTime plantedAt; // 심은 날짜

    @Column
    private LocalDateTime germinatedAt; // 발아 날짜

    @Column
    private LocalDateTime maturedAt; // 성숙 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantStage plantStage; // 성장 단계

    // Diary와의 1:N 관계
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Diary> diaries = new ArrayList<>();

    // Device와의 1:1 관계 설정
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_serial", unique = true, nullable = false)
    private Device device;

    // Species와의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    public Plant(PlantRequestDto plantRequestDto, Species species, Device device) {
        this.name = plantRequestDto.getName();
        this.plantedAt = plantRequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDto.getMaturedAt();
        this.plantStage = plantRequestDto.getPlantStage();
        this.species = species;
        this.device = device;

    }

    public void update(PlantRequestDto plantRequestDtorequestDto) {
        this.name = plantRequestDtorequestDto.getName();
        this.plantStage = plantRequestDtorequestDto.getPlantStage();
        this.plantedAt = plantRequestDtorequestDto.getPlantedAt();
        this.germinatedAt = plantRequestDtorequestDto.getGerminatedAt();
        this.maturedAt = plantRequestDtorequestDto.getMaturedAt();
    }
}
