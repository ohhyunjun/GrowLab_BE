package com.metaverse.growlab_be.harvest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class HarvestInferenceClient {
    private final RestTemplate restTemplate;

    @Value("${inference.server.url:http://localhost:5000}")
    private String inferenceServerUrl;

    public HarvestPayload predict(HarvestPayload input) {
        var body = Map.of(
                "features", input.features(),
                "anchor", input.anchor(),
                "anchorAt", input.anchorAt().toString(),
                "predictedAt", input.predictedAt().toString(),
                "modelVersion", input.modelVersion(),
                "featureVersion", input.featureVersion());
        Response result;
        try {
            result = restTemplate.postForObject(inferenceServerUrl + "/predict", body, Response.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "수확기 예측 서버에 연결할 수 없습니다.", ex);
        }
        if (result == null || !"PREDICTED".equals(result.status())
                || !HarvestService.MODEL.equals(result.modelVersion())
                || !HarvestService.FEATURES.equals(result.featureVersion())
                || result.remainingDays() == null || !Double.isFinite(result.remainingDays())
                || result.remainingDays() < 0 || result.expectedAt() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "수확기 예측 서버 응답이 올바르지 않습니다.");
        }
        double actual = Duration.between(input.predictedAt(), result.expectedAt()).toMillis() / 1000.0;
        if (Math.abs(actual - result.remainingDays() * 86400) > 1) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "수확기 예상일과 잔여일이 일치하지 않습니다.");
        }
        return new HarvestPayload(input.schemaVersion(), input.observationId(), input.capturedAt(),
                input.predictedAt(), result.modelVersion(), result.featureVersion(), "PREDICTED",
                input.anchor(), input.anchorAt(), result.remainingDays(), result.expectedAt(),
                input.features(), input.confirmedStage());
    }

    public record Response(String status, Double remainingDays, Instant expectedAt,
                           String modelVersion, String featureVersion) {}
}
