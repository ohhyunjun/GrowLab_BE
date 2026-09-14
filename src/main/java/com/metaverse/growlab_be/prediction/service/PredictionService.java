package com.metaverse.growlab_be.prediction.service;

import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import com.metaverse.growlab_be.prediction.dto.PredictionRequestDto;
import com.metaverse.growlab_be.prediction.dto.PredictionResponseDto;
import com.metaverse.growlab_be.prediction.repository.PredictionRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PlantRepository      plantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePrediction(PredictionRequestDto dto) {
        Plant plant = plantRepository.findById(dto.getPlant_id())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식물입니다."));
        Species species = plant.getDevice().getSpecies();
        if (species == null) {
            throw new IllegalArgumentException("해당 식물의 품종 정보가 없습니다.");
        }

        //시계열 예측 반환값 검증(잘못된 값이 있는지 없는지 등)
        validatePredictionResult(plant, species, dto.getPredicted_stage());
        validatePredictionRange(dto.getEta_lower_hours(), dto.getEta_point_hours(), dto.getEta_upper_hours());
        validateConfidence(dto.getConfidence());

        Prediction prediction = new Prediction(
                plant,
                dto.getPredicted_stage(),
                dto.getConfidence(),
                dto.getEta_lower_hours(),
                dto.getEta_point_hours(),
                dto.getEta_upper_hours()
        );
        prediction.setPredictionMode(dto.getPrediction_mode());
        predictionRepository.save(prediction);
    }

    @Transactional(readOnly = true)
    public PredictionResponseDto getLatestPrediction(Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식물입니다."));

        return predictionRepository
                .findTopByPlantOrderByCreatedAtDesc(plant)
                .map(this::buildScheduleResponse)
                .orElse(null);
    }

    // 저장된 예측값 + 품종 재배 일정을 조합해 날짜/보정치를 계산한 응답을 만든다
    private PredictionResponseDto buildScheduleResponse(Prediction prediction) {
        Plant plant = prediction.getPlant();
        Species species = plant.getDevice().getSpecies();

        // 구형 예측 기록은 새 ETA 컬럼이 비어 있으므로 새 예측이 쌓일 때까지 표시하지 않는다.
        if (prediction.getCreatedAt() == null
                || prediction.getEtaLowerHours() == null
                || prediction.getEtaPointHours() == null
                || prediction.getEtaUpperHours() == null) {
            return null;
        }

        // 단계 변경 후 남아 있는 이전 단계의 예측을 다음 단계 예측으로 보여주지 않는다.
        int currentStage = plant.getStageIndex() == null ? 0 : plant.getStageIndex();
        if (prediction.getPredictedStage() == null
                || prediction.getPredictedStage() != currentStage + 1
                || (species != null && prediction.getPredictedStage() >= species.getStageCount())) {
            return null;
        }

        LocalDateTime predictedAt = prediction.getCreatedAt();

        // 품종 정보 없이도 계산 가능한 AI 예측 기반 날짜
        LocalDateTime expectedAt   = predictedAt.plusHours(prediction.getEtaPointHours());
        LocalDateTime rangeStartAt = predictedAt.plusHours(prediction.getEtaLowerHours());
        LocalDateTime rangeEndAt   = predictedAt.plusHours(prediction.getEtaUpperHours());

        // 품종의 재배 일정이 있어야 계산 가능한 값
        String predictedStageName = null;
        LocalDateTime baselineAt   = null;
        Long correctionHours       = null;

        if (species != null && plant.getPlantedAt() != null) {
            Integer predictedStage = prediction.getPredictedStage();
            predictedStageName = species.getStageName(predictedStage);
            baselineAt = plant.getPlantedAt()
                    .plusDays(species.getStageStartDay(predictedStage));
            correctionHours = Duration.between(baselineAt, expectedAt).toHours();
        }

        return new PredictionResponseDto(
                prediction,
                predictedStageName,
                correctionHours,
                baselineAt,
                expectedAt,
                rangeStartAt,
                rangeEndAt
        );
    }

    private void validatePredictionResult(Plant plant, Species species, Integer predictedStage) {
        if (predictedStage == null){
            throw new IllegalArgumentException("예측 단계가 누락되었습니다.");
        }
        int currentStage = plant.getStageIndex() == null ? 0 : plant.getStageIndex();
        int lastStage = species.getStageCount() - 1;

        if (currentStage >= lastStage){
            throw new IllegalStateException("마지막 생육단계 입니다. 더이상 예측 불가능합니다.");
        }
        if (predictedStage < 0 || predictedStage > lastStage){
            throw new IllegalArgumentException("불가능한 단계 범위 입니다.");
        }

        int nextStage = currentStage + 1;
        if (predictedStage != nextStage){
            throw new IllegalArgumentException("예측한 단계가 현재단계의 다음 단계가 아닙니다.");
        }
    }

    private void validatePredictionRange(Integer lowerEtaHours, Integer pointEtaHours, Integer upperEtaHours) {
        if (lowerEtaHours == null || pointEtaHours == null || upperEtaHours == null) {
            throw new IllegalArgumentException("예측 시간 범위가 누락되었습니다.");
        }
        if (lowerEtaHours < 0 || pointEtaHours < 0 || upperEtaHours < 0) {
            throw new IllegalArgumentException("예측 시간은 0 이상이어야 합니다.");
        }
        if (lowerEtaHours > pointEtaHours || pointEtaHours > upperEtaHours) {
            throw new IllegalArgumentException("예측 시간은 lower <= point <= upper 순서여야 합니다.");
        }
    }

    private void validateConfidence(Float confidence) {
        if (confidence == null) {
            throw new IllegalArgumentException("예측 신뢰도가 누락되었습니다.");
        }
        if (!Float.isFinite(confidence) || confidence < 0.0f || confidence > 1.0f) {
            throw new IllegalArgumentException("예측 신뢰도는 0 이상 1 이하여야 합니다.");
        }
    }
}
