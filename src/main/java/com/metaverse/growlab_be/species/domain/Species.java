package com.metaverse.growlab_be.species.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.plant.domain.Plant;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 종 이름 (예: "방울토마토", "적상추")
    @Column(nullable = false, unique = true)
    private String name;

    // 발아 후 성체까지 걸리는 평균 일수
    @Column(nullable = false)
    private Integer daysToMature;

    // AI 분석을 위한 가이드라인 텍스트
    @Column(length = 500)
    private String aiPromptGuideline;

}
