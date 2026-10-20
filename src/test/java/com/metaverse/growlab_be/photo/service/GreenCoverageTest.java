package com.metaverse.growlab_be.photo.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GreenCoverageTest {
    @Test
    void 박스_안의_녹색만_전체_화면_대비_비율로_센다() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++) for (int x = 0; x < 10; x++) image.setRGB(x, y, 0x00aa00);
        for (int y = 2; y < 8; y++) for (int x = 2; x < 8; x++) image.setRGB(x, y, 0x002200);
        for (int y = 3; y < 7; y++) for (int x = 3; x < 7; x++) image.setRGB(x, y, 0x00aa00);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MockMultipartFile file = new MockMultipartFile("imageFile", "plant.png", "image/png", bytes.toByteArray());
        PlantExtent.Result extent = PlantExtent.of("""
                [{"cls":"Growing","conf":0.9,"x":0.5,"y":0.5,"w":0.6,"h":0.6}]""");

        assertThat(GreenCoverage.frameRatio(file, extent)).isEqualTo(0.16);
    }

    @Test
    void 넓은_박스_안에서도_중앙_식물과_떨어진_옆_포트_잎은_뺀다() throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        for (int y = 8; y < 12; y++) for (int x = 8; x < 12; x++) image.setRGB(x, y, 0x00aa00);
        for (int y = 8; y < 12; y++) for (int x = 15; x < 19; x++) image.setRGB(x, y, 0x00aa00);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MockMultipartFile file = new MockMultipartFile("imageFile", "plants.png", "image/png", bytes.toByteArray());
        PlantExtent.Result extent = PlantExtent.of("""
                [{"conf":0.95,"x":0.5,"y":0.5,"w":1.0,"h":1.0}]""");

        assertThat(GreenCoverage.frameRatio(file, extent)).isEqualTo(0.04);
    }
}
