package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.photo.service.PlantSeries.Observation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 성장 추이 계산에 쓰는 순수 계산. */
final class GrowthMath {

    private GrowthMath() {}

    private static final int MIN_RATE_OBSERVATION_DAYS = 3;
    private static final int MIN_RATE_SPAN_DAYS = 2;
    // 이보다 가까운 사진끼리는 비교하지 않는다. 몇 시간 간격의 작은 흔들림이 하루 성장률로 몇 배 부풀려진다.
    private static final double MIN_PAIR_GAP_DAYS = 20.0 / 24;

    /**
     * 하루 성장률(%). 사진 두 장씩 찍힌 시각 차이로 로그 기울기를 구해 그 중앙값을 쓴다.
     *
     * 하루 값을 먼저 하나로 줄인 뒤 날짜끼리 비교하지 않는 이유: 식물은 하루 사이에도 자라서 새벽 사진이
     * 저녁 사진보다 작게 나오므로, 사진이 한두 장뿐인 날은 그 시각에 따라 하루 값이 치우친다.
     * 사진을 찍힌 시각 그대로 비교하면 그런 치우침이 없고, 비교 쌍이 많아져 튀는 사진에도 덜 흔들린다.
     *
     * 묶음은 같은 위치에서 본 사진들이다. 묶음 사이의 값 차이는 성장으로 치지 않는다.
     * 한 묶음이라도 관측일이 3일 이상이고 처음과 끝이 이틀 이상 벌어져야 값을 낸다.
     */
    static Double ratePerDayPercent(List<List<Observation>> sameViewGroups) {
        boolean enough = false;
        List<Double> slopes = new ArrayList<>();
        for (List<Observation> group : sameViewGroups) {
            if (group.isEmpty()) continue;
            long firstDay = group.stream().mapToLong(Observation::elapsedDay).min().getAsLong();
            long lastDay = group.stream().mapToLong(Observation::elapsedDay).max().getAsLong();
            if (group.stream().mapToLong(Observation::elapsedDay).distinct().count() >= MIN_RATE_OBSERVATION_DAYS
                    && lastDay - firstDay >= MIN_RATE_SPAN_DAYS) enough = true;
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    double gapDays = Math.abs(Duration.between(group.get(i).shotAt(), group.get(j).shotAt())
                            .toMinutes()) / 1440.0;
                    if (gapDays < MIN_PAIR_GAP_DAYS) continue;
                    Observation earlier = group.get(i).shotAt().isBefore(group.get(j).shotAt()) ? group.get(i) : group.get(j);
                    Observation later = earlier == group.get(i) ? group.get(j) : group.get(i);
                    slopes.add((Math.log(later.size()) - Math.log(earlier.size())) / gapDays);
                }
            }
        }
        if (!enough || slopes.isEmpty()) return null;
        Collections.sort(slopes);
        return (Math.exp(percentile(slopes, 0.50)) - 1) * 100;
    }

    /** 오름차순으로 정렬된 값의 분위수(0~1). 값 사이는 직선으로 잇는다. */
    static double percentile(List<Double> sorted, double p) {
        double index = p * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        return sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * (index - lower);
    }
}
