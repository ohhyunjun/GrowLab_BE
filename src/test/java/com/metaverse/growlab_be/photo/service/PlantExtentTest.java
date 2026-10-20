package com.metaverse.growlab_be.photo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlantExtentTest {

    // 운영 사진(2026-10-08 17:12, 1번 포트)의 실제 검출: 식물 박스들과, 타워 옆 방 배경을 잡은 큰 박스.
    private static final String PLANT_AND_BACKGROUND = """
            [{"cls":"Planting","conf":0.96,"x":0.248,"y":0.593,"w":0.268,"h":0.306},
             {"cls":"Planting","conf":0.85,"x":0.197,"y":0.610,"w":0.165,"h":0.092},
             {"cls":"Planting","conf":0.67,"x":0.282,"y":0.538,"w":0.070,"h":0.196},
             {"cls":"Planting","conf":0.75,"x":0.331,"y":0.696,"w":0.100,"h":0.098},
             {"cls":"Growing","conf":0.34,"x":0.770,"y":0.507,"w":0.420,"h":0.810}]""";

    @Test
    void 식물과_떨어진_큰_배경_박스는_범위에_넣지_않는다() {
        PlantExtent.Result extent = PlantExtent.of(PLANT_AND_BACKGROUND);

        assertThat(extent.areaRatio()).isCloseTo(0.268 * 0.306, within(0.002));
        assertThat(extent.cut()).isFalse();
    }

    @Test
    void 신뢰도_높은_잎을_감싼_식물_전체_박스는_신뢰도가_낮아도_범위가_된다() {
        // 2026-10-10 17:13, 4번 포트: 잎 박스(0.62)가 가장 확실하고 식물 전체 박스는 0.46.
        PlantExtent.Result extent = PlantExtent.of("""
                [{"cls":"Planting","conf":0.62,"x":0.47,"y":0.50,"w":0.28,"h":0.28},
                 {"cls":"Planting","conf":0.46,"x":0.62,"y":0.66,"w":0.59,"h":0.60},
                 {"cls":"Harvest","conf":0.44,"x":0.75,"y":0.81,"w":0.29,"h":0.27}]""");

        assertThat(extent.areaRatio()).isCloseTo(0.59 * 0.60, within(0.01));
        assertThat(extent.cut()).isFalse();
    }

    @Test
    void 화면_가장자리에_닿은_범위는_잘린_것으로_표시한다() {
        PlantExtent.Result bottom = PlantExtent.of(
                "[{\"cls\":\"Planting\",\"conf\":0.92,\"x\":0.44,\"y\":0.87,\"w\":0.16,\"h\":0.26}]");
        PlantExtent.Result left = PlantExtent.of(
                "[{\"cls\":\"Planting\",\"conf\":0.91,\"x\":0.13,\"y\":0.38,\"w\":0.26,\"h\":0.52}]");

        assertThat(bottom.cut()).isTrue();
        assertThat(left.cut()).isTrue();
    }

    @Test
    void 서로_떨어진_잎_박스는_가장_확실한_것만_남긴다() {
        PlantExtent.Result extent = PlantExtent.of("""
                [{"cls":"Planting","conf":0.39,"x":0.30,"y":0.30,"w":0.20,"h":0.20},
                 {"cls":"Planting","conf":0.36,"x":0.72,"y":0.83,"w":0.30,"h":0.26}]""");

        assertThat(extent.areaRatio()).isCloseTo(0.04, within(1e-9));
    }

    @Test
    void 검출_목록이_없거나_읽을_수_없으면_판단하지_않는다() {
        assertThat(PlantExtent.of(null)).isNull();
        assertThat(PlantExtent.of("")).isNull();
        assertThat(PlantExtent.of("[]")).isNull();
        assertThat(PlantExtent.of("not json")).isNull();
        assertThat(PlantExtent.of("[{\"cls\":\"Planting\",\"conf\":0.9}]")).isNull();
    }

    @Test
    void 광각_사진은_옆_포트의_더_높은_신뢰도보다_중앙_포트의_식물을_고른다() {
        PlantExtent.Result target = PlantExtent.of("""
                [{"conf":0.98,"x":0.87,"y":0.40,"w":0.18,"h":0.20},
                 {"conf":0.73,"x":0.39,"y":0.47,"w":0.10,"h":0.25}]""", true);
        assertThat(target).isNotNull();
        assertThat(target.centerX()).isCloseTo(0.39, within(0.001));
        assertThat(target.areaRatio()).isCloseTo(0.025, within(0.001));
        assertThat(target.confidence()).isEqualTo(0.73);
    }

    @Test
    void 광각_사진에서_옆_포트만_검출되면_이_포트의_성장량을_만들지_않는다() {
        assertThat(PlantExtent.of("""
                [{"conf":0.99,"x":0.86,"y":0.45,"w":0.16,"h":0.20}]""", true)).isNull();
    }

    @Test
    void 광각_사진의_위쪽_잎이_화면_경계에_거의_닿으면_잘린_최소_크기로_본다() {
        PlantExtent.Result extent = PlantExtent.of("""
                [{"conf":0.9658,"x":0.51623,"y":0.44151,"w":0.92668,"h":0.86218}]""", true);
        assertThat(extent.cut()).isTrue();
        assertThat(extent.areaRatio()).isCloseTo(.79897, within(.0001));
    }
}
