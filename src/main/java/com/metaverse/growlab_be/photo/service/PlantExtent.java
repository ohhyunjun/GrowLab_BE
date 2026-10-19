package com.metaverse.growlab_be.photo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 사진 한 장의 검출 박스 목록에서 "이 포트의 식물이 차지한 범위"를 고른다.
 *
 * 가장 큰 박스를 그대로 쓰면 타워 옆으로 보이는 방 배경을 잡은 큰 박스가 식물 크기로 들어간다.
 * 그래서 신뢰도가 가장 높은 박스를 식물로 보고, 그 박스와 겹치는 박스(식물 전체 박스와 그 안의 잎 박스)만
 * 한 묶음으로 이어 그 묶음을 둘러싼 범위를 크기로 쓴다. 식물과 떨어져 있는 박스는 버린다.
 */
final class PlantExtent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 작은 쪽 박스의 이 비율 이상이 겹치면 같은 식물의 박스로 본다.
    private static final double LINK_OVERLAP = 0.3;
    // 범위가 화면 가장자리의 이 안쪽까지 닿으면 식물이 화면 밖으로 잘린 것으로 본다.
    private static final double EDGE_MARGIN = 0.01;

    /**
     * areaRatio: 프레임 대비 범위 면적(0~1). cut: 화면 밖으로 잘려 실제 크기를 알 수 없음.
     * centerX/Y: 화면에서 범위의 중심(0~1). 포트를 옮겨 찍히는 자리가 바뀌었는지 판단하는 데 쓴다.
     */
    record Result(double areaRatio, boolean cut, double centerX, double centerY) {}

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
        List<Box> boxes = parse(detectionsJson);
        if (boxes.isEmpty()) return null;

        Box anchor = boxes.get(0);
        for (Box box : boxes) if (box.confidence() > anchor.confidence()) anchor = box;

        List<Box> plant = new ArrayList<>(List.of(anchor));
        List<Box> rest = new ArrayList<>(boxes);
        rest.remove(anchor);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Box candidate : new ArrayList<>(rest)) {
                if (plant.stream().anyMatch(candidate::linkedTo)) {
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
        return new Result((right - left) * (bottom - top), cut, (left + right) / 2, (top + bottom) / 2);
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
