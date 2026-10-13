package com.metaverse.growlab_be.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 포트별 성장 추이. 크기는 이 포트 자신의 과거와 견주고, 속도는 같은 품종의 다른 재배 기록과 견준다.
 * 크기는 신뢰도 70% 이상인 사진의 검출 박스에서 고른 식물 범위의 프레임 대비 면적 비율(0~1)이다.
 * 박스 면적은 실제 식물 크기가 아니며, 근거가 부족한 항목은 값을 채우지 않는다.
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
    private final SizeChange sizeChange;   // 이 포트가 며칠 전보다 얼마나 커졌는지
    private final GrowthRate growthRate;   // 이 포트의 최근 성장 속도. 다른 재배와 견주는 데 쓴다
    private final Reference reference;     // 같은 기기·같은 품종의 다른 재배 기록이 같은 시기에 보인 속도와의 비교
    private final Long viewChangedDay;     // 포트를 옮겨 찍히는 자리가 마지막으로 바뀐 경과일. 바뀐 적 없으면 null

    public GrowthTrendResponseDto(Integer portIndex, List<Point> points, List<Point> peerPoints,
                                  Integer rank, Integer portCount, Double gapPercent, Double latestRatio,
                                  List<PortHealth> ports, SizeChange sizeChange, GrowthRate growthRate,
                                  Reference reference,
                                  Long viewChangedDay) {
        this.portIndex = portIndex;
        this.points = points;
        this.peerPoints = peerPoints;
        this.rank = rank;
        this.portCount = portCount;
        this.gapPercent = gapPercent;
        this.latestRatio = latestRatio;
        this.ports = ports;
        this.sizeChange = sizeChange;
        this.growthRate = growthRate;
        this.reference = reference;
        this.viewChangedDay = viewChangedDay;
    }

    @Getter
    @AllArgsConstructor
    public static class Point {
        private final String date;       // yyyy-MM-dd (화면 라벨용)
        private final Long elapsedDay;   // 파종 후 경과일 — 또래 비교의 기준축
        private final Double value;      // boxAreaRatio (0~1)
    }

    /**
     * 이 포트의 과거 대비 크기. 최근 7일 안에서 가장 이른 날과 가장 최근 날의 하루 값을 견준다.
     * 포트를 옮겨 찍히는 자리가 바뀌었으면 그 뒤의 날끼리만 견준다. ready=false면 나머지 값은 null.
     */
    @Getter
    @AllArgsConstructor
    public static class SizeChange {
        private final boolean ready;
        private final Integer days;           // 며칠 전과 견줬는지
        private final Double fromRatio;       // 그날의 크기(프레임 대비 0~1)
        private final Double toRatio;         // 가장 최근 날의 크기
        private final Double changePercent;   // (toRatio / fromRatio - 1) * 100

        public static SizeChange pending() {
            return new SizeChange(false, null, null, null, null);
        }
    }

    /**
     * 최근 7일 관측으로 구한 하루 성장률(%). 찍히는 자리가 바뀐 뒤에는 그 뒤 관측만으로 계산한다.
     * status: pending(관측 부족) · growing(성장) · flat(정체) · down(관측 크기 감소).
     */
    @Getter
    @AllArgsConstructor
    public static class GrowthRate {
        private final Double ratePerDay;          // 최근 구간, null 가능
        private final Double previousRatePerDay;  // 그 직전 7일 구간(참고용), null 가능
        private final String status;
        private final Integer observationDays;    // 계산에 쓴 관측일 수
        private final Integer spanDays;           // 첫 관측과 마지막 관측 사이 일수

        public static GrowthRate pending() {
            return new GrowthRate(null, null, "pending", null, null);
        }
    }

    /**
     * 같은 품종의 다른 재배 기록이 심은 뒤 같은 시기에 보인 하루 성장률(%)과의 비교. ready=false면 나머지 값은 null.
     * verdict: faster(더 빠름) · similar(비슷함) · slower(더 느림). 크기가 아니라 속도를 견준 결과다.
     */
    @Getter
    @AllArgsConstructor
    public static class Reference {
        private final boolean ready;
        private final Double ratePerDay;      // 다른 재배 기록들의 중앙값
        private final Double lowRatePerDay;   // 하위 25%
        private final Double highRatePerDay;  // 상위 25%
        private final Integer plantCount;     // 비교에 들어간 재배 기록 수(자기 자신 제외)
        private final String verdict;

        public static Reference notReady() {
            return new Reference(false, null, null, null, null, null);
        }
    }

    /** health: pending(비교 대기) · good(같은 시기와 비슷하거나 빠름) · warn(같은 시기보다 느림) · bad(질병). */
    @Getter
    @AllArgsConstructor
    public static class PortHealth {
        private final Integer portIndex;
        private final String health;
    }
}
