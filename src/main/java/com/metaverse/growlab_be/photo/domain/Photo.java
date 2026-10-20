package com.metaverse.growlab_be.photo.domain;

import com.metaverse.growlab_be.common.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(name="uk_photo_observation",columnNames={"device_serial","observation_id"}),
       indexes=@Index(name="idx_photo_plant_captured",columnList="plant_id,captured_at"))
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

    // ── 전체 식물 박스 크기(관측값). 겹쳐 나오는 잎 박스는 제외하고 "최대 면적 박스" 기준.
    //    RPi가 아직 안 보내는 사진도 있으므로 모두 nullable. 픽셀값은 해상도 의존이라
    //    비교에는 boxAreaRatio(프레임 대비 비율)를 우선 사용한다.
    @Column
    private Double boxWidth;      // 박스 가로 (픽셀)

    @Column
    private Double boxHeight;     // 박스 세로 (픽셀)

    @Column
    private Double boxArea;       // 박스 면적 (픽셀^2)

    @Column
    private Double boxAreaRatio;  // 박스 면적 / 프레임 면적 (0~1)

    @Column(name = "green_area_ratio", nullable = true)
    private Double greenAreaRatio; // 대상 포트 식물 박스 안의 녹색 픽셀 / 전체 프레임 면적

    // ── 검출된 전체 박스 원본. 정규화 좌표(xywhn) 기준 JSON 배열: [{cls, conf, x, y, w, h}, ...]
    //    대표 박스 선택 규칙(현재: 최대 면적)이 나중에 바뀌어도 재검증할 수 있게 전부 남긴다.
    @Column(columnDefinition = "TEXT")
    private String detectionsJson;

    @Column
    private Integer selectedBoxIndex; // detectionsJson 배열에서 대표로 고른 박스의 인덱스

    private Long plantId;
    private java.time.Instant capturedAt;
    @Column(length=36)
    private String observationId;

    public void bindCapture(Long plantId, java.time.Instant capturedAt, String observationId) {
        this.plantId=plantId; this.capturedAt=capturedAt; this.observationId=observationId;
    }

    // 직접 선언 (크기/검출 없이 저장하던 기존 흐름 호환용 → 해당 필드는 null)
    public Photo(Device device, Integer portIndex, String filePath, String fileName,
                 String growthStageResult, Double confidence,
                 String diseaseResult, Double diseaseConfidence) {
        this(device, portIndex, filePath, fileName,
                growthStageResult, confidence, diseaseResult, diseaseConfidence,
                null, null, null, null, null, null);
    }

    // 크기(관측값) + 전체 검출(JSON)까지 함께 저장하는 생성자
    public Photo(Device device, Integer portIndex, String filePath, String fileName,
                 String growthStageResult, Double confidence,
                 String diseaseResult, Double diseaseConfidence,
                 Double boxWidth, Double boxHeight, Double boxArea, Double boxAreaRatio,
                 String detectionsJson, Integer selectedBoxIndex) {
        this(device, portIndex, filePath, fileName, growthStageResult, confidence,
                diseaseResult, diseaseConfidence, boxWidth, boxHeight, boxArea, boxAreaRatio,
                detectionsJson, selectedBoxIndex, null);
    }

    public Photo(Device device, Integer portIndex, String filePath, String fileName,
                 String growthStageResult, Double confidence,
                 String diseaseResult, Double diseaseConfidence,
                 Double boxWidth, Double boxHeight, Double boxArea, Double boxAreaRatio,
                 String detectionsJson, Integer selectedBoxIndex, Double greenAreaRatio) {
        this.device            = device;
        this.portIndex         = portIndex;
        this.filePath          = filePath;
        this.fileName          = fileName;
        this.growthStageResult = growthStageResult;
        this.confidence        = confidence;
        this.diseaseResult     = diseaseResult;
        this.diseaseConfidence = diseaseConfidence;
        this.boxWidth          = boxWidth;
        this.boxHeight         = boxHeight;
        this.boxArea           = boxArea;
        this.boxAreaRatio      = boxAreaRatio;
        this.greenAreaRatio    = greenAreaRatio;
        this.detectionsJson    = detectionsJson;
        this.selectedBoxIndex  = selectedBoxIndex;
    }

    public void recordGreenAreaRatio(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0 || ratio > 1) throw new IllegalArgumentException("Invalid green area ratio");
        this.greenAreaRatio = ratio;
    }
}
