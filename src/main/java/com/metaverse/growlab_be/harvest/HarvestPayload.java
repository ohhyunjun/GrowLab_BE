package com.metaverse.growlab_be.harvest;

import java.time.Instant;
import java.util.List;

public record HarvestPayload(Integer schemaVersion, String observationId, Instant capturedAt,
        Instant predictedAt, String modelVersion, String featureVersion, String status,
        String anchor, Instant anchorAt, Double remainingDays, Instant expectedAt, List<Double> features, Integer confirmedStage) {}
