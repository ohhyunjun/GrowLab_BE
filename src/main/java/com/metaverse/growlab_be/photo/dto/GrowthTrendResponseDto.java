package com.metaverse.growlab_be.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 포트별 성장 추이(관측 크기) + 또래 비교 결과.
 * 신뢰도 70% 이상인 YOLO 박스의 boxAreaRatio(프레임 대비 면적 비율, 0~1)를 쓴다.
 * 박스 면적은 실제 식물 크기가 아니며, 비교 근거가 부족하면 순위·격차를 null로 반환한다.
 */
@Getter
public class GrowthTrendResponseDto {

    private final Integer portIndex;
    private final List<Point> points;      // 이 포트의 일자별 관측 크기 (오래된→최신)
    private final List<Point> peerPoints;  // 같은 '경과일'의 다른 활성 포트 평균 (LOCF, 최대 3일 공백까지)
    private final Integer rank;            // 같은 경과일 기준 이 포트의 순위 (1 = 가장 큼)
    private final Integer portCount;       // 같은 경과일에서 실제로 비교된 포트 수(자기 자신 포함)
    private final Double gapPercent;       // (이 포트 최신 - 또래 평균 최신) / 또래 평균 최신 * 100, null 가능
    private final Double latestRatio;      // 이 포트 최신 관측 크기, null 가능
    private final List<PortHealth> ports;  // 기기 전체 포트의 상태. 근거 부족은 pending.

    public GrowthTrendResponseDto(Integer portIndex, List<Point> points, List<Point> peerPoints,
                                  Integer rank, Integer portCount, Double gapPercent, Double latestRatio,
                                  List<PortHealth> ports) {
        this.portIndex = portIndex;
        this.points = points;
        this.peerPoints = peerPoints;
        this.rank = rank;
        this.portCount = portCount;
        this.gapPercent = gapPercent;
        this.latestRatio = latestRatio;
        this.ports = ports;
    }

    @Getter
    @AllArgsConstructor
    public static class Point {
        private final String date;       // yyyy-MM-dd (화면 라벨용)
        private final Long elapsedDay;   // 파종 후 경과일 — 또래 비교의 기준축
        private final Double value;      // boxAreaRatio (0~1)
    }

    /** health: pending(비교 대기) · good(비교 정상) · warn(비교 주의) · bad(질병). */
    @Getter
    @AllArgsConstructor
    public static class PortHealth {
        private final Integer portIndex;
        private final String health;
    }
}
