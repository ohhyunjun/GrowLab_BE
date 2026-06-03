package com.metaverse.growlab_be.photo.domain;

import com.metaverse.growlab_be.common.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Photo extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_serial", referencedColumnName = "serial_number")
    private Device device;

    @Column(nullable = false)
    private Integer portIndex; // 포트 번호

    @Column(nullable = false, length = 512)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String growthStageResult; //생육단계 결과

    @Column
    private Double confidence; // 예: 0.95 (95% 신뢰도)

    @Column
    private String diseaseResult; // 질병탐지 결과

    @Column
    private Double diseaseConfidence;

    // 직접 선언
    public Photo(Device device, Integer portIndex, String filePath, String fileName,
                 String growthStageResult, Double confidence,
                 String diseaseResult, Double diseaseConfidence) {
        this.device            = device;
        this.portIndex         = portIndex;
        this.filePath          = filePath;
        this.fileName          = fileName;
        this.growthStageResult = growthStageResult;
        this.confidence        = confidence;
        this.diseaseResult     = diseaseResult;
        this.diseaseConfidence = diseaseConfidence;
    }
}