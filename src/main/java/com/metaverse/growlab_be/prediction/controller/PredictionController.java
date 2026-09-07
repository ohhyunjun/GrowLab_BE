package com.metaverse.growlab_be.prediction.controller;

import com.metaverse.growlab_be.prediction.dto.PredictionRequestDto;
import com.metaverse.growlab_be.prediction.dto.PredictionResponseDto;
import com.metaverse.growlab_be.prediction.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    // 추론 서버 → 예측 결과 저장
    @PostMapping
    public ResponseEntity<Void> savePrediction(@RequestBody PredictionRequestDto dto) {
        try {
            predictionService.savePrediction(dto);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalStateException e) {
            // 이미 마지막 생육 단계인 경우
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (IllegalArgumentException e) {
            // 누락값, 잘못된 단계, ETA 범위 오류 등
            return ResponseEntity.badRequest().build();
        }
    }

    // 프론트 → plant_id 기준 최신 예측 조회
    @GetMapping("/{plantId}")
    public ResponseEntity<PredictionResponseDto> getLatestPrediction(
            @PathVariable Long plantId) {
        try {
            PredictionResponseDto dto = predictionService.getLatestPrediction(plantId);
            if (dto == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            // 해당 plantId의 식물이 없는 경우
            return ResponseEntity.notFound().build();
        }
    }
}