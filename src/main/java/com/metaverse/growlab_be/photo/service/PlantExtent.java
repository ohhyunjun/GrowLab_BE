package com.metaverse.growlab_be.photo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;

/**
 * 사진 한 장의 검출 박스 목록에서 "이 포트의 식물이 차지한 범위"를 고른다.
 *
 * 기존 카메라의 선택 규칙은 유지한다. 광각 카메라는 첫 유효 사진에서만 중앙 대상의
 * 연결된 전체 범위를 찾고, 이후에는 같은 재배의 이전 범위와 겹침·거리로 연결한다.
 * 중심이 움직여도 가로·세로 각각 화면의 25%까지 허용하며 중앙 영역으로 박스를 자르지 않는다.
 * 멀리 이동한 범위는 반복 확인하고, 새 식물 등록 시에는 별도 추적기를 쓴다.
 */
final class PlantExtent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 작은 쪽 박스의 이 비율 이상이 겹치면 같은 식물의 박스로 본다.
    private static final double LINK_OVERLAP = 0.3;
    // 범위가 화면 가장자리의 이 안쪽까지 닿으면 식물이 화면 밖으로 잘린 것으로 본다.
    private static final double EDGE_MARGIN = 0.02;
    // 새 광각 카메라에서 촬영 대상 재배 구멍이 놓이는 영역. 옆 포트 잎의 중심은 이 밖이다.
    private static final double TARGET_LEFT = 0.28;
    private static final double TARGET_RIGHT = 0.72;
    private static final double TARGET_TOP = 0.18;
    private static final double TARGET_BOTTOM = 0.72;

    /**
     * areaRatio: 프레임 대비 범위 면적(0~1). cut: 화면 밖으로 잘려 실제 크기를 알 수 없음.
     * centerX/Y: 화면에서 범위의 중심(0~1). 포트를 옮겨 찍히는 자리가 바뀌었는지 판단하는 데 쓴다.
     */
    record Result(double areaRatio, boolean cut, double centerX, double centerY,
                  double left, double top, double right, double bottom, double confidence) {}

    private record Box(double confidence, double left, double top, double right, double bottom) {
        double area() { return (right - left) * (bottom - top); }

        boolean linkedTo(Box other) {
            double width = Math.min(right, other.right) - Math.max(left, other.left);
            double height = Math.min(bottom, other.bottom) - Math.max(top, other.top);
            if (width <= 0 || height <= 0) return false;
            return width * height / Math.min(area(), other.area()) >= LINK_OVERLAP;
        }
    }

    private PlantExtent() {}

    /** 검출 목록(JSON: cls, conf, 정규화된 중심 x·y와 w·h)이 없거나 읽을 수 없으면 null. */
    static Result of(String detectionsJson) {
        return of(detectionsJson, false);
    }

    /** wideCamera=true: only the plant rooted in the central target port is eligible. */
    static Result of(String detectionsJson, boolean wideCamera) {
        return new Tracker().select(detectionsJson, wideCamera, 0);
    }

    // Reconstruct the exact region used by older releases before reusing their green-pixel count.
    static Result legacyOf(String detectionsJson, boolean wideCamera) {
        List<Box> boxes = parse(detectionsJson);
        if (boxes.isEmpty()) return null;

        Box anchor = null;
        for (Box box : boxes) {
            double cx = (box.left() + box.right()) / 2;
            double cy = (box.top() + box.bottom()) / 2;
            if (wideCamera && (cx < TARGET_LEFT || cx > TARGET_RIGHT || cy < TARGET_TOP || cy > TARGET_BOTTOM)) continue;
            if (anchor == null || box.confidence() > anchor.confidence()) anchor = box;
        }
        if (anchor == null) return null;

        List<Box> plant = new ArrayList<>(List.of(anchor));
        List<Box> rest = new ArrayList<>(boxes);
        rest.remove(anchor);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Box candidate : new ArrayList<>(rest)) {
                if ((!wideCamera || centerInTarget(candidate)) && plant.stream().anyMatch(candidate::linkedTo)) {
                    plant.add(candidate);
                    rest.remove(candidate);
                    grew = true;
                }
            }
        }

        double left = 1, top = 1, right = 0, bottom = 0;
        for (Box box : plant) {
            left = Math.min(left, box.left());
            top = Math.min(top, box.top());
            right = Math.max(right, box.right());
            bottom = Math.max(bottom, box.bottom());
        }
        boolean cut = left <= EDGE_MARGIN || top <= EDGE_MARGIN
                || right >= 1 - EDGE_MARGIN || bottom >= 1 - EDGE_MARGIN;
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(1, right);
        bottom = Math.min(1, bottom);
        return new Result((right - left) * (bottom - top), cut, (left + right) / 2, (top + bottom) / 2,
                left, top, right, bottom, anchor.confidence());
    }

    static String regionKey(Result extent) {
        return extent == null ? null : String.format(Locale.ROOT, "tracked-v2:%.6f,%.6f,%.6f,%.6f",
                extent.left(), extent.top(), extent.right(), extent.bottom());
    }

    static boolean sameRegion(Result a, Result b) {
        return a != null && b != null && Math.abs(a.left() - b.left()) < .00001
                && Math.abs(a.top() - b.top()) < .00001 && Math.abs(a.right() - b.right()) < .00001
                && Math.abs(a.bottom() - b.bottom()) < .00001;
    }

    /** One cultivation, replayed in capture order. No state is shared across ports or new plants. */
    static final class Tracker {
        // Frame-relative movement, up to 25% of width AND 25% of height; never a clipping region.
        static final double MAX_MOVEMENT = .25;
        private Result previous;
        private Result relocation;
        private Boolean previousWide;

        Result select(String json, boolean wide, double minConfidence) {
            if (previousWide != null && previousWide != wide) { previous = null; relocation = null; }
            previousWide = wide;
            // Old-camera observations retain their established selection rule.
            if (!wide) return legacyOf(json, false);
            List<Box> boxes = parse(json);
            List<Result> candidates = components(boxes).stream()
                    .filter(r -> r.confidence() >= minConfidence).toList();
            if (candidates.isEmpty()) { relocation = null; return null; }
            if (previous == null) {
                // Bootstrap only: a central leaf can identify its entire connected plant,
                // including enclosing boxes whose centers lie outside the old central window.
                Box seed = boxes.stream().filter(PlantExtent::centerInTarget)
                        .filter(b -> b.confidence() >= minConfidence)
                        .max(Comparator.comparingDouble(Box::confidence)).orElse(null);
                if (seed == null) return null;
                Result first = candidates.stream().filter(r -> contains(r, seed))
                        .max(Comparator.comparingDouble(Result::areaRatio)).orElse(null);
                return accept(first);
            }
            Result match = candidates.stream().filter(r -> near(previous, r, MAX_MOVEMENT))
                    .max(Comparator.comparingDouble(r -> affinity(previous, r))).orElse(null);
            if (match != null) return accept(match);

            // A distant detection must repeat before it becomes the new viewing position.
            // A single neighbor/false detection never moves the tracked plant.
            Result moved = candidates.stream().max(Comparator.comparingDouble(r -> affinity(previous, r))).orElseThrow();
            if (relocation != null && near(relocation, moved, .08)
                    && moved.areaRatio() / relocation.areaRatio() >= .5
                    && moved.areaRatio() / relocation.areaRatio() <= 2) return accept(moved);
            relocation = moved;
            return null;
        }

        private Result accept(Result result) {
            if (result != null) previous = result;
            relocation = null;
            return result;
        }

        private static boolean near(Result a, Result b, double limit) {
            return Math.abs(a.centerX() - b.centerX()) <= limit
                    && Math.abs(a.centerY() - b.centerY()) <= limit;
        }

        private static double affinity(Result a, Result b) {
            double intersection = Math.max(0, Math.min(a.right(), b.right()) - Math.max(a.left(), b.left()))
                    * Math.max(0, Math.min(a.bottom(), b.bottom()) - Math.max(a.top(), b.top()));
            double iou = intersection / (a.areaRatio() + b.areaRatio() - intersection);
            return 2 * iou - Math.hypot(a.centerX() - b.centerX(), a.centerY() - b.centerY())
                    + .01 * b.confidence();
        }
    }

    private static boolean contains(Result result, Box box) {
        return result.left() <= Math.max(0, box.left()) + 1e-9
                && result.right() >= Math.min(1, box.right()) - 1e-9
                && result.top() <= Math.max(0, box.top()) + 1e-9
                && result.bottom() >= Math.min(1, box.bottom()) - 1e-9;
    }

    private static List<Result> components(List<Box> boxes) {
        List<Box> rest = new ArrayList<>(boxes);
        List<Result> results = new ArrayList<>();
        while (!rest.isEmpty()) {
            List<Box> connected = new ArrayList<>(List.of(rest.remove(0)));
            boolean grew = true;
            while (grew) {
                grew = false;
                for (Box candidate : new ArrayList<>(rest)) {
                    if (connected.stream().anyMatch(candidate::linkedTo)) {
                        connected.add(candidate); rest.remove(candidate); grew = true;
                    }
                }
            }
            double left = Math.max(0, connected.stream().mapToDouble(Box::left).min().orElseThrow());
            double top = Math.max(0, connected.stream().mapToDouble(Box::top).min().orElseThrow());
            double right = Math.min(1, connected.stream().mapToDouble(Box::right).max().orElseThrow());
            double bottom = Math.min(1, connected.stream().mapToDouble(Box::bottom).max().orElseThrow());
            if (right <= left || bottom <= top) continue;
            results.add(new Result((right - left) * (bottom - top),
                    left <= EDGE_MARGIN || top <= EDGE_MARGIN || right >= 1 - EDGE_MARGIN || bottom >= 1 - EDGE_MARGIN,
                    (left + right) / 2, (top + bottom) / 2, left, top, right, bottom,
                    connected.stream().mapToDouble(Box::confidence).max().orElseThrow()));
        }
        return results;
    }

    private static boolean centerInTarget(Box box) {
        double x = (box.left() + box.right()) / 2;
        double y = (box.top() + box.bottom()) / 2;
        return x >= TARGET_LEFT && x <= TARGET_RIGHT && y >= TARGET_TOP && y <= TARGET_BOTTOM;
    }

    private static List<Box> parse(String detectionsJson) {
        List<Box> boxes = new ArrayList<>();
        if (detectionsJson == null || detectionsJson.isBlank()) return boxes;
        try {
            JsonNode root = MAPPER.readTree(detectionsJson);
            if (!root.isArray()) return boxes;
            for (JsonNode node : root) {
                double x = node.path("x").asDouble(Double.NaN);
                double y = node.path("y").asDouble(Double.NaN);
                double w = node.path("w").asDouble(Double.NaN);
                double h = node.path("h").asDouble(Double.NaN);
                double confidence = node.path("conf").asDouble(Double.NaN);
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(confidence)
                        || !(w > 0) || !(h > 0) || !Double.isFinite(w) || !Double.isFinite(h)) continue;
                boxes.add(new Box(confidence, x - w / 2, y - h / 2, x + w / 2, y + h / 2));
            }
        } catch (Exception unreadable) {
            boxes.clear();
        }
        return boxes;
    }
}
