package com.metaverse.growlab_be.photo.service;

/** Frame-relative visual footprint. It is not physical leaf area or biomass. */
final class VisualSizeIndex {
    private static final double BOX_WEIGHT = 0.70;
    private static final double GREEN_WEIGHT = 1.0 - BOX_WEIGHT;

    private VisualSizeIndex() {}

    static Double combine(double boxRatio, Double greenRatio) {
        if (!Double.isFinite(boxRatio) || boxRatio <= 0 || boxRatio > 1
                || greenRatio == null || !Double.isFinite(greenRatio)
                || greenRatio <= 0 || greenRatio > boxRatio + 0.01) return null;
        return Math.pow(boxRatio, BOX_WEIGHT) * Math.pow(greenRatio, GREEN_WEIGHT);
    }
}
