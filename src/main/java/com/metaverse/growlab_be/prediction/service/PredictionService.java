package com.metaverse.growlab_be.prediction.service;

import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import com.metaverse.growlab_be.prediction.dto.PredictionRequestDto;
import com.metaverse.growlab_be.prediction.dto.PredictionResponseDto;
import com.metaverse.growlab_be.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PlantRepository      plantRepository;

    @Transactional
    public void savePrediction(PredictionRequestDto dto) {
        Plant plant = plantRepository.findById(dto.getPlant_id())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식물입니다."));

        Prediction prediction = new Prediction(
                plant,
                dto.getPredicted_stage(),
                dto.getConfidence(),
                dto.getGermination_eta_hours(),
                dto.getMature_eta_hours()
        );
        predictionRepository.save(prediction);
    }

    @Transactional(readOnly = true)
    public PredictionResponseDto getLatestPrediction(Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식물입니다."));

        return predictionRepository
                .findTopByPlantOrderByCreatedAtDesc(plant)
                .map(PredictionResponseDto::new)
                .orElse(null);
    }
}