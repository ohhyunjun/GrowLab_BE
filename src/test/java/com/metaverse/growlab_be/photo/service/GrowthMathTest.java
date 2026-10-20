package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.photo.service.PlantSeries.Observation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GrowthMathTest {

    private static final LocalDateTime PLANTED = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Test
    void 분위수는_정렬된_값_사이를_직선으로_잇는다() {
        List<Double> sorted = List.of(0.12, 0.16, 0.20);

        assertThat(GrowthMath.percentile(sorted, 0.50)).isCloseTo(0.16, within(1e-9));
        assertThat(GrowthMath.percentile(sorted, 0.25)).isCloseTo(0.14, within(1e-9));
        assertThat(GrowthMath.percentile(sorted, 0.75)).isCloseTo(0.18, within(1e-9));
    }

    @Test
    void 성장률은_사진이_찍힌_시각_차이로_하루_기준을_낸다() {
        // 하루 10%씩 자라는 식물. 날마다 찍힌 시각과 장수가 다르다.
        List<Observation> photos = List.of(growing(1, 5), growing(1, 23), growing(3, 11), growing(6, 5), growing(6, 17));

        assertThat(GrowthMath.ratePerDayPercent(List.of(photos))).isCloseTo(10.0, within(1e-6));
    }

    @Test
    void 몇_시간_간격의_사진끼리는_비교하지_않는다() {
        // 같은 날 안에서는 크게 흔들리지만 날짜를 건너면 하루 10%씩 자란다.
        List<Observation> photos = new ArrayList<>();
        for (int day = 1; day <= 4; day++) {
            photos.add(photo(day, 5, 0.10 * Math.pow(1.10, day - 1)));
            photos.add(photo(day, 11, 0.10 * Math.pow(1.10, day - 1) * 1.5));
        }

        // 05시끼리, 11시끼리, 05시→다음날 11시(30시간), 11시→다음날 05시(18시간, 제외)의 쌍 중앙값
        Double rate = GrowthMath.ratePerDayPercent(List.of(photos));

        assertThat(rate).isNotNull();
        assertThat(rate).isBetween(5.0, 25.0);
    }

    @Test
    void 관측일이_셋_미만이거나_이틀이_안_벌어지면_성장률이_없다() {
        assertThat(GrowthMath.ratePerDayPercent(List.of(List.of(growing(1, 12), growing(5, 12))))).isNull();
        assertThat(GrowthMath.ratePerDayPercent(List.of(List.of(growing(1, 5), growing(1, 23), growing(2, 12))))).isNull();
        assertThat(GrowthMath.ratePerDayPercent(List.of(List.<Observation>of()))).isNull();
    }

    @Test
    void 묶음_사이의_값_차이는_성장으로_치지_않는다() {
        List<Observation> before = new ArrayList<>();
        List<Observation> after = new ArrayList<>();
        for (int day = 1; day <= 3; day++) before.add(photo(day, 12, 0.20 * Math.pow(1.10, day - 1)));
        for (int day = 4; day <= 6; day++) after.add(photo(day, 12, 0.05 * Math.pow(1.10, day - 4)));

        assertThat(GrowthMath.ratePerDayPercent(List.of(before, after))).isCloseTo(10.0, within(1e-6));
        assertThat(GrowthMath.ratePerDayPercent(List.of(
                List.of(photo(1, 12, 0.10), photo(2, 12, 0.11)), List.of(photo(3, 12, 0.05), photo(4, 12, 0.06))))).isNull();
    }

    // 파종 뒤 흐른 시간만큼 하루 10%씩 자란 크기로 찍힌 사진.
    private Observation growing(int day, int hour) {
        return photo(day, hour, 0.05 * Math.pow(1.10, day + hour / 24.0));
    }

    private Observation photo(int day, int hour, double size) {
        return new Observation(PLANTED.plusDays(day).withHour(hour), day, size, false, 0.3, 0.6);
    }
}
