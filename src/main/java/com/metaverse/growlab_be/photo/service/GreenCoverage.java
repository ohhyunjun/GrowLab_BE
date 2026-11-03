package com.metaverse.growlab_be.photo.service;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

/** Green pixels inside the target port's detection, with detached neighboring foliage excluded. */
final class GreenCoverage {
    private static final long MAX_PIXELS = 24_000_000;

    private GreenCoverage() {}

    static Double frameRatio(MultipartFile file, PlantExtent.Result extent) throws IOException {
        if (extent == null || file == null || file.isEmpty()) return null;
        try (InputStream input = file.getInputStream()) {
            return frameRatio(input, extent);
        }
    }

    static Double frameRatio(InputStream input, PlantExtent.Result extent) throws IOException {
        if (extent == null || input == null) return null;
        BufferedImage image = ImageIO.read(input);
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) return null;

        int left = Math.max(0, Math.min(width, (int) Math.floor(extent.left() * width)));
        int top = Math.max(0, Math.min(height, (int) Math.floor(extent.top() * height)));
        int right = Math.max(0, Math.min(width, (int) Math.ceil(extent.right() * width)));
        int bottom = Math.max(0, Math.min(height, (int) Math.ceil(extent.bottom() * height)));
        if (right <= left || bottom <= top) return null;

        int cropWidth = right - left;
        int cropHeight = bottom - top;
        BitSet mask = new BitSet(cropWidth * cropHeight);
        int green = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                if (g > 40 && g * 100 > red * 102 && g * 100 > blue * 105
                        && 2 * g - red - blue > 12) {
                    mask.set((y - top) * cropWidth + x - left);
                    green++;
                }
            }
        }
        // A very wide YOLO box can cover a neighboring port too. The target plant must
        // remain connected to the center of the detected plant; detached green is ignored.
        if (extent.areaRatio() > 0.5 && green > 0) {
            green = centeredComponent(mask, cropWidth, cropHeight, left, top, width, height, extent);
            if (green == 0) return null; // No evidence that the green belongs to this port.
        }
        return (double) green / ((long) width * height);
    }

    private static int centeredComponent(BitSet mask, int cropWidth, int cropHeight,
                                         int left, int top, int width, int height,
                                         PlantExtent.Result extent) {
        BitSet visited = new BitSet(cropWidth * cropHeight);
        int[] queue = new int[Math.min(mask.cardinality(), 65536)];
        int bestHits = 0;
        int bestCount = 0;
        for (int first = mask.nextSetBit(0); first >= 0; first = mask.nextSetBit(first + 1)) {
            if (visited.get(first)) continue;
            int head = 0, tail = 0, hits = 0;
            queue[tail++] = first;
            visited.set(first);
            while (head < tail) {
                int pixel = queue[head++];
                int x = pixel % cropWidth;
                int y = pixel / cropWidth;
                double nx = (left + x + 0.5) / width;
                double ny = (top + y + 0.5) / height;
                if (Math.abs(nx - extent.centerX()) <= 0.12
                        && Math.abs(ny - extent.centerY()) <= 0.12) hits++;
                for (int dy = -1; dy <= 1; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= cropHeight) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= cropWidth || (dx == 0 && dy == 0)) continue;
                        int neighbor = yy * cropWidth + xx;
                        if (!mask.get(neighbor) || visited.get(neighbor)) continue;
                        if (tail == queue.length) queue = java.util.Arrays.copyOf(queue, Math.min(mask.cardinality(), queue.length * 2));
                        queue[tail++] = neighbor;
                        visited.set(neighbor);
                    }
                }
            }
            if (hits > bestHits || (hits == bestHits && tail > bestCount)) {
                bestHits = hits;
                bestCount = tail;
            }
        }
        return bestHits == 0 ? 0 : bestCount;
    }
}
