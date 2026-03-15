package com.metaverse.growlab_be.plant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plant")
public class Plant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime plantedAt; // 심은 날짜

    private LocalDateTime germinatedAt; // 발아 날짜

    @Column(nullable = false)
    private String plantStage; // 성장 단계
}
