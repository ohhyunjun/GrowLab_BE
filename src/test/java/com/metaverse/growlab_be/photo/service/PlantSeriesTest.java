package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.photo.service.PlantSeries.Observation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlantSeriesTest {

    private static final LocalDateTime PLANTED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Test
    void 포트를_옮겨_찍히는_자리가_바뀌면_그_전후를_따로_본다() {
        List<Observation> observations = new ArrayList<>();
        for (int day = 1; day <= 4; day++) observations.add(at(day, 12, 0.10 * Math.pow(1.10, day - 1), 0.30, 0.65));
        // 5일차에 포트를 옮겨 식물이 절반만 보이게 됐지만, 같은 속도로 자라는 중이다.
        for (int day = 5; day <= 8; day++) observations.add(at(day, 12, 0.05 * Math.pow(1.10, day - 5), 0.60, 0.40));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.daily()).hasSize(8);
        assertThat(series.lastViewChangeDay()).isEqualTo(5L);
        assertThat(series.sameViewPhotos(1, 8)).hasSize(2);
        assertThat(series.latestViewPhotos(2, 8).get(0).elapsedDay()).isEqualTo(5L);
        assertThat(GrowthMath.ratePerDayPercent(List.of(series.latestViewPhotos(2, 8)))).isCloseTo(10.0, within(1e-6));
        assertThat(GrowthMath.ratePerDayPercent(series.sameViewPhotos(1, 8))).isCloseTo(10.0, within(1e-6));
    }

    @Test
    void 자리가_그대로면_위치_변화로_보지_않는다() {
        // 같은 자리에서도 박스 중심은 조금씩 흔들리고, 식물이 자라며 천천히 옮겨 간다.
        List<Observation> observations = List.of(
                at(1, 12, 0.10, 0.30, 0.65), at(2, 12, 0.11, 0.33, 0.62), at(3, 12, 0.12, 0.27, 0.66),
                at(4, 12, 0.13, 0.31, 0.60), at(5, 12, 0.14, 0.28, 0.58), at(6, 12, 0.15, 0.26, 0.56));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.lastViewChangeDay()).isNull();
        assertThat(series.sameViewPhotos(1, 6)).hasSize(1);
    }

    @Test
    void 한_장만_동떨어진_자리에_찍힌_박스는_버린다() {
        List<Observation> observations = List.of(
                at(1, 12, 0.10, 0.30, 0.65), at(2, 12, 0.11, 0.30, 0.65),
                at(3, 6, 0.40, 0.78, 0.50),                              // 엉뚱한 곳을 잡은 한 장
                at(3, 12, 0.12, 0.30, 0.65), at(4, 12, 0.13, 0.30, 0.65));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.lastViewChangeDay()).isNull();
        assertThat(series.daily().get(3L)).isEqualTo(0.12);
    }

    @Test
    void 마지막_한_장의_중심_변화만으로_포트_이동을_확정하지_않는다() {
        List<Observation> observations = List.of(
                at(1, 12, .10, .50, .55),
                at(2, 12, .12, .50, .55),
                at(2, 21, .13, .51, .44));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.lastViewChangeDay()).isNull();
        assertThat(series.daily().get(2L)).isCloseTo(.125, within(1e-9));
    }

    @Test
    void 잘린_사진도_그날_크기의_하한으로_남긴다() {
        List<Observation> observations = List.of(
                at(1, 6, 0.10, 0.30, 0.65), at(1, 12, 0.11, 0.30, 0.65), cut(1, 18, 0.08, 0.30, 0.70),
                at(2, 6, 0.16, 0.30, 0.65), cut(2, 12, 0.09, 0.30, 0.70));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.daily().get(1L)).isCloseTo(0.10, within(1e-9));
        assertThat(series.daily().get(2L)).isCloseTo(0.125, within(1e-9));
        assertThat(series.isLowerBound(2L)).isTrue();
        assertThat(series.hasExactPhoto(2L)).isTrue();
    }

    @Test
    void 카메라_교체는_박스_중심이_같아도_촬영_구간을_나눈다() {
        List<Observation> observations = List.of(
                new Observation(PLANTED.plusDays(1), 1, .10, false, .5, .5, .10, null, 0),
                new Observation(PLANTED.plusDays(2), 2, .11, false, .5, .5, .11, null, 0),
                new Observation(PLANTED.plusDays(3), 3, .05, false, .5, .5, .05, null, 1),
                new Observation(PLANTED.plusDays(4), 4, .06, false, .5, .5, .06, null, 1));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.lastViewChangeDay()).isEqualTo(3L);
        assertThat(series.latestViewDaily(1, 4)).containsOnlyKeys(3L, 4L);
        assertThat(series.sameViewPhotos(1, 4)).hasSize(2);
    }

    @Test
    void 같은_날_자리가_바뀌면_새_자리의_관측만_그날_값이_된다() {
        List<Observation> observations = List.of(
                at(1, 12, 0.10, 0.30, 0.65), at(2, 6, 0.11, 0.30, 0.65),
                at(2, 12, 0.06, 0.60, 0.40), at(2, 18, 0.06, 0.60, 0.40), at(3, 12, 0.07, 0.60, 0.40));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.lastViewChangeDay()).isEqualTo(2L);
        assertThat(series.daily().get(2L)).isEqualTo(0.06);
        assertThat(series.latestViewPhotos(1, 3)).extracting(Observation::elapsedDay).containsExactly(2L, 2L, 3L);
    }

    @Test
    void 검출_목록이_없는_옛_사진은_자리를_알_수_없어_한_구간으로_둔다() {
        List<Observation> observations = List.of(
                new Observation(PLANTED.plusDays(1), 1, 0.10, false, null, null),
                new Observation(PLANTED.plusDays(2), 2, 0.30, false, null, null));

        PlantSeries series = PlantSeries.of(observations);

        assertThat(series.daily()).hasSize(2);
        assertThat(series.lastViewChangeDay()).isNull();
    }

    private Observation at(int day, int hour, double size, double x, double y) {
        return new Observation(PLANTED.plusDays(day).withHour(hour), day, size, false, x, y);
    }

    private Observation cut(int day, int hour, double size, double x, double y) {
        return new Observation(PLANTED.plusDays(day).withHour(hour), day, size, true, x, y);
    }
}
