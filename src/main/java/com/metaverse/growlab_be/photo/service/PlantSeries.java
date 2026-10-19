package com.metaverse.growlab_be.photo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 식물 한 개체의 관측을 경과일별 크기로 정리한다.
 *
 * 포트(화분)를 손으로 옮기면 같은 포트라도 화면에 찍히는 자리와 보이는 범위가 달라져, 식물이 그대로여도
 * 크기 값이 한 번에 달라진다. 그래서 식물이 화면에서 찍힌 자리가 크게 바뀐 시점을 "보는 위치가 바뀐 것"으로
 * 구분해 둔다. 크기 자체는 구간을 넘어 그대로 보여주되, 성장 속도는 같은 구간 안의 관측끼리만 비교한다.
 *
 * 하루 값(그래프·크기 비교용)은 그날 사진의 중앙값이고, 성장 속도는 하루 값이 아니라 사진 하나하나로 계산한다
 * ({@link GrowthMath#ratePerDayPercent}).
 */
final class PlantSeries {

    /** centerX/Y: 화면에서 식물 범위의 중심(0~1). 검출 목록이 없는 옛 사진은 null. */
    record Observation(LocalDateTime shotAt, long elapsedDay, double size, boolean cut,
                       Double centerX, Double centerY) {}

    // 식물 중심이 화면의 이 비율 이상 옮겨 가면 위치가 바뀐 것으로 본다. 같은 자리에서는 대부분 6% 안에서 움직인다.
    static final double VIEW_SHIFT = 0.10;
    // 현재 위치의 기준은 최근 관측 몇 장의 중심 중앙값이다. 식물이 자라며 중심이 천천히 옮겨 가는 것은 따라간다.
    private static final int VIEW_MEMORY = 6;

    private final TreeMap<Long, Double> daily = new TreeMap<>();
    private final Map<Long, Integer> viewByDay = new HashMap<>();
    // 경과일 -> 그날 값에 들어간 사진들(잘리지 않은 것). 성장 속도 계산에 쓴다.
    private final TreeMap<Long, List<Observation>> photosByDay = new TreeMap<>();
    private Long lastViewChangeDay;

    private PlantSeries() {}

    static PlantSeries of(List<Observation> observations) {
        List<Observation> ordered = new ArrayList<>(observations);
        ordered.sort(Comparator.comparing(Observation::shotAt));

        PlantSeries series = new PlantSeries();
        // 경과일 -> 그날 마지막으로 본 위치의 관측들
        TreeMap<Long, List<Observation>> byDay = new TreeMap<>();
        Map<Long, Integer> lastViewOfDay = new HashMap<>();
        List<double[]> recentCenters = new ArrayList<>();
        int view = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Observation o = ordered.get(i);
            if (o.centerX() != null && !recentCenters.isEmpty()
                    && distance(o, median(recentCenters, 0), median(recentCenters, 1)) > VIEW_SHIFT) {
                Observation next = nextWithCenter(ordered, i + 1);
                // 다음 사진도 새 자리에 있어야 옮긴 것으로 본다. 한 장만 동떨어져 있으면 엉뚱한 박스로 보고 버린다.
                if (next != null && distance(next, o.centerX(), o.centerY()) > VIEW_SHIFT) continue;
                view++;
                recentCenters.clear();
                series.lastViewChangeDay = o.elapsedDay();
            }
            if (o.centerX() != null) {
                recentCenters.add(new double[]{o.centerX(), o.centerY()});
                if (recentCenters.size() > VIEW_MEMORY) recentCenters.remove(0);
            }
            Integer dayView = lastViewOfDay.put(o.elapsedDay(), view);
            if (dayView != null && dayView != view) byDay.remove(o.elapsedDay());   // 같은 날 위치가 바뀌면 새 위치의 관측만 남긴다
            byDay.computeIfAbsent(o.elapsedDay(), k -> new ArrayList<>()).add(o);
        }

        for (Map.Entry<Long, List<Observation>> day : byDay.entrySet()) {
            List<Observation> whole = new ArrayList<>();
            for (Observation o : day.getValue()) if (!o.cut()) whole.add(o);
            int cut = day.getValue().size() - whole.size();
            // 그날 사진의 절반 이상에서 식물이 화면 밖으로 잘렸으면 나머지 값도 믿기 어려워 그날은 비운다.
            if (whole.isEmpty() || cut >= whole.size()) continue;
            List<Double> sizes = new ArrayList<>();
            for (Observation o : whole) sizes.add(o.size());
            Collections.sort(sizes);
            // 그래프에 찍는 하루 값. 잎만 잡힌 한 장이 끌어내리지 않게 평균이 아니라 중앙값을 쓴다.
            series.daily.put(day.getKey(), GrowthMath.percentile(sizes, 0.50));
            series.viewByDay.put(day.getKey(), lastViewOfDay.get(day.getKey()));
            series.photosByDay.put(day.getKey(), whole);
        }
        return series;
    }

    /** 경과일 -> 그날의 크기. */
    TreeMap<Long, Double> daily() { return daily; }

    /** 지금 보는 위치가 시작된 경과일. 위치가 한 번도 바뀌지 않았으면 null. */
    Long lastViewChangeDay() { return lastViewChangeDay; }

    /** 구간 [fromDay, toDay] 중 가장 최근에 본 위치의 하루 값만. 크기는 같은 위치에서 본 것끼리만 견줄 수 있다. */
    NavigableMap<Long, Double> latestViewDaily(long fromDay, long toDay) {
        NavigableMap<Long, Double> window = daily.subMap(fromDay, true, toDay, true);
        NavigableMap<Long, Double> latest = new TreeMap<>();
        if (window.isEmpty()) return latest;
        int view = viewByDay.get(window.lastKey());
        for (Map.Entry<Long, Double> e : window.entrySet()) {
            if (viewByDay.get(e.getKey()) == view) latest.put(e.getKey(), e.getValue());
        }
        return latest;
    }

    /** 구간 [fromDay, toDay]의 사진을 보는 위치별로 나눈다(오래된 위치부터). 성장 속도는 같은 묶음 안에서만 계산한다. */
    List<List<Observation>> sameViewPhotos(long fromDay, long toDay) {
        Map<Integer, List<Observation>> groups = new TreeMap<>();
        for (Map.Entry<Long, List<Observation>> e : photosByDay.subMap(fromDay, true, toDay, true).entrySet()) {
            groups.computeIfAbsent(viewByDay.get(e.getKey()), k -> new ArrayList<>()).addAll(e.getValue());
        }
        return new ArrayList<>(groups.values());
    }

    /** 구간 [fromDay, toDay] 중 가장 최근에 본 위치의 사진만. */
    List<Observation> latestViewPhotos(long fromDay, long toDay) {
        List<List<Observation>> groups = sameViewPhotos(fromDay, toDay);
        return groups.isEmpty() ? List.of() : groups.get(groups.size() - 1);
    }

    private static Observation nextWithCenter(List<Observation> ordered, int from) {
        for (int i = from; i < ordered.size(); i++) {
            if (ordered.get(i).centerX() != null) return ordered.get(i);
        }
        return null;
    }

    private static double distance(Observation o, double x, double y) {
        return Math.hypot(o.centerX() - x, o.centerY() - y);
    }

    private static double median(List<double[]> centers, int axis) {
        List<Double> values = new ArrayList<>();
        for (double[] center : centers) values.add(center[axis]);
        Collections.sort(values);
        return GrowthMath.percentile(values, 0.50);
    }
}
