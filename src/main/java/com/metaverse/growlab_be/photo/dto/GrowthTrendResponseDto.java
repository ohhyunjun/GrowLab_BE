package com.metaverse.growlab_be.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 포트별 성장 추이(관측 크기) + 또래 비교 결과.
 * 값은 boxAreaRatio(프레임 대비 박스 면적 비율, 0~1)를 쓴다. 해상도에 독립적이라 포트 간 비교에 안전.
 * 로컬/초기에는 박스크기 데이터가 없어 points/peerPoints가 비어 올 수 있다(프론트는 "데이터 없음" 처리).
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
    private final List<PortHealth> ports;  // 기기 전체 포트의 상태(정상/주의/이상). 선택 포트와 무관하게 항상 채운다 → 포트 선택기 점 색에 사용(추가 호출 불필요)

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

    /** 포트 하나의 종합 상태. health: good(정상) · warn(성장정체 주의) · bad(질병). 식물 없는 포트는 목록에서 제외(프론트가 빈 포트 처리). */
    @Getter
    @AllArgsConstructor
    public static class PortHealth {
        private final Integer portIndex;
        private final String health;
    }
}
