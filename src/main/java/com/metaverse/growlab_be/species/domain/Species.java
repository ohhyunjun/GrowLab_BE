package com.metaverse.growlab_be.species.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.species.dto.SpeciesRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "species")
public class Species extends TimeStamped {

    private static final List<String> DEFAULT_STAGE_NAMES = List.of("씨앗", "발아", "수확");
    private static final List<Integer> DEFAULT_STAGE_DURATIONS = List.of(0, 7, 14);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int daysToMature;

    @Column(length = 500)
    private String aiPromptGuideline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "min_temperature")
    private Double minTemperature;

    @Column(name = "max_temperature")
    private Double maxTemperature;

    @Column(name = "min_humidity")
    private Double minHumidity;

    @Column(name = "max_humidity")
    private Double maxHumidity;

    @Column(name = "min_ph")
    private Double minPh;

    @Column(name = "max_ph")
    private Double maxPh;

    @Column(name = "min_tds")
    private Double minTds;

    @Column(name = "max_tds")
    private Double maxTds;

    @Column(name = "min_light_hours")
    private Double minLightHours;

    @Column(name = "max_light_hours")
    private Double maxLightHours;

    // 이 품종의 생육 단계 이름 목록 (순서 있음, 개수 가변)
    @ElementCollection
    @CollectionTable(name = "species_stage", joinColumns = @JoinColumn(name = "species_id"))
    @OrderColumn(name = "stage_order")
    @Column(name = "stage_name", nullable = false)
    private List<String> stageNames = new ArrayList<>();

    // ✅ 각 단계가 "재배 시작 후 며칠째부터" 시작되는지 (stageNames와 같은 순서/개수로 매칭)
    // 예: [0, 7, 14] -> 0일째 첫 단계, 7일째부터 둘째 단계, 14일째부터 셋째 단계
    @ElementCollection
    @CollectionTable(name = "species_stage_duration", joinColumns = @JoinColumn(name = "species_id"))
    @OrderColumn(name = "stage_order")
    @Column(name = "start_day", nullable = false)
    private List<Integer> stageDurationDays = new ArrayList<>();

    public Species(SpeciesRequestDto speciesRequestDto) {
        applyFrom(speciesRequestDto);
    }

    public void update(SpeciesRequestDto speciesRequestDto) {
        applyFrom(speciesRequestDto);
    }

    private void applyFrom(SpeciesRequestDto dto) {
        this.name = dto.getName();
        this.daysToMature = dto.getDaysToMature();
        this.aiPromptGuideline = dto.getAiPromptGuideline();
        this.category = dto.getCategory();
        this.difficulty = dto.getDifficulty();
        this.minTemperature = dto.getMinTemperature();
        this.maxTemperature = dto.getMaxTemperature();
        this.minHumidity = dto.getMinHumidity();
        this.maxHumidity = dto.getMaxHumidity();
        this.minPh = dto.getMinPh();
        this.maxPh = dto.getMaxPh();
        this.minTds = dto.getMinTds();
        this.maxTds = dto.getMaxTds();
        this.minLightHours = dto.getMinLightHours();
        this.maxLightHours = dto.getMaxLightHours();

        if (dto.getStageNames() != null && !dto.getStageNames().isEmpty()) {
            this.stageNames = new ArrayList<>(dto.getStageNames());
        } else if (this.stageNames == null || this.stageNames.isEmpty()) {
            this.stageNames = new ArrayList<>(DEFAULT_STAGE_NAMES);
        }

        // ✅ 단계별 시작일. 단계명 개수와 다르게 오면 무시하고 기본값/기존값 유지 (불일치 방지)
        if (dto.getStageDurationDays() != null
                && !dto.getStageDurationDays().isEmpty()
                && dto.getStageDurationDays().size() == this.stageNames.size()) {
            this.stageDurationDays = new ArrayList<>(dto.getStageDurationDays());
        } else if (this.stageDurationDays == null
                || this.stageDurationDays.isEmpty()
                || this.stageDurationDays.size() != this.stageNames.size()) {
            this.stageDurationDays = buildDefaultDurations(this.stageNames.size());
        }
    }

    private List<Integer> buildDefaultDurations(int stageCount) {
        if (stageCount == DEFAULT_STAGE_DURATIONS.size()) {
            return new ArrayList<>(DEFAULT_STAGE_DURATIONS);
        }
        // 단계 수가 기본값과 다르면 0, 7, 14, 21... 처럼 7일 간격으로 자동 생성
        List<Integer> generated = new ArrayList<>();
        for (int i = 0; i < stageCount; i++) {
            generated.add(i * 7);
        }
        return generated;
    }

    public int getStageCount() {
        return stageNames == null || stageNames.isEmpty() ? DEFAULT_STAGE_NAMES.size() : stageNames.size();
    }

    public String getStageName(int index) {
        List<String> names = (stageNames == null || stageNames.isEmpty()) ? DEFAULT_STAGE_NAMES : stageNames;
        int safeIndex = Math.max(0, Math.min(index, names.size() - 1));
        return names.get(safeIndex);
    }

    // ✅ index번째 단계가 시작되는 일수 (재배 시작일 기준)
    public int getStageStartDay(int index) {
        List<Integer> durations = (stageDurationDays == null || stageDurationDays.isEmpty())
                ? DEFAULT_STAGE_DURATIONS : stageDurationDays;
        int safeIndex = Math.max(0, Math.min(index, durations.size() - 1));
        return durations.get(safeIndex);
    }

    // ✅ 재배 시작 후 daysElapsed일이 지났을 때, 날짜 기준으로 도달해야 할 단계 인덱스 계산
    // (뒤에서부터 훑어서 daysElapsed 이상인 가장 마지막 단계를 찾음)
    public int resolveStageIndexByDays(int daysElapsed) {
        int count = getStageCount();
        for (int i = count - 1; i >= 0; i--) {
            if (daysElapsed >= getStageStartDay(i)) {
                return i;
            }
        }
        return 0;
    }
}