package com.metaverse.growlab_be.prediction.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import com.metaverse.growlab_be.prediction.dto.PredictionRequestDto;
import com.metaverse.growlab_be.prediction.dto.PredictionResponseDto;
import com.metaverse.growlab_be.prediction.repository.PredictionRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Spring 서버나 DB를 실행하지 않는 조회/날짜 계산 단위 테스트.
class PredictionServiceTest {
    private PredictionRepository predictionRepository;
    private PlantRepository plantRepository;
    private PredictionService predictionService;
    private Plant plant;
    private Prediction prediction;

    @BeforeEach
    void setUp() {
        predictionRepository = mock(PredictionRepository.class);
        plantRepository = mock(PlantRepository.class);
        predictionService = new PredictionService(predictionRepository, plantRepository);
        Species species = new Species();
        species.setStageNames(List.of("파종", "육묘", "개화", "착과", "수확"));
        species.setStageDurationDays(List.of(0, 7, 20, 30, 60));
        Device device = new Device();
        plant = new Plant();
        plant.setSpecies(species);
        plant.setId(1L);
        plant.setDevice(device);
        plant.setStageIndex(1);
        plant.setPlantedAt(LocalDateTime.of(2026, 8, 1, 9, 0));
        prediction = new Prediction(plant, 2, 0.8f, 24, 48, 72);
        prediction.setPredictionMode("LIGHTGBM_AI");
        ReflectionTestUtils.setField(prediction, "createdAt", LocalDateTime.of(2026, 8, 21, 9, 0));
        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
        when(predictionRepository.findTopByPlantOrderByCreatedAtDesc(plant)).thenReturn(Optional.of(prediction));
    }

    @Test
    void calculatesNextStageDatesFromSpeciesAndModel() {
        PredictionResponseDto response = predictionService.getLatestPrediction(1L);
        assertNotNull(response);
        assertEquals("개화", response.getPredictedStageName());
        assertEquals(plant.getPlantedAt().plusDays(20), response.getBaselineAt());
        assertEquals(prediction.getCreatedAt().plusHours(48), response.getExpectedAt());
        assertEquals(prediction.getCreatedAt().plusHours(24), response.getRangeStartAt());
        assertEquals(prediction.getCreatedAt().plusHours(72), response.getRangeEndAt());
        assertEquals(48L, response.getCorrectionHours());
        assertEquals("LIGHTGBM_AI", response.getPredictionMode());
    }

    @Test
    void hidesLegacyRowsWithoutEtaWithoutDeletingThem() {
        prediction.setEtaPointHours(null);
        assertNull(predictionService.getLatestPrediction(1L));
        verify(predictionRepository, never()).delete(any(Prediction.class));
    }

    @Test
    void hidesPredictionAfterPlantStageChanges() {
        plant.setStageIndex(2);
        assertNull(predictionService.getLatestPrediction(1L));
    }

    @Test
    void finalStageHasNoNextPrediction() {
        plant.setStageIndex(4);
        prediction.setPredictedStage(5);
        assertNull(predictionService.getLatestPrediction(1L));
    }

    @Test
    void noPredictionReturnsNoContent() {
        when(predictionRepository.findTopByPlantOrderByCreatedAtDesc(plant)).thenReturn(Optional.empty());
        assertNull(predictionService.getLatestPrediction(1L));
    }

    @Test
    void savesValidFlaskPredictionContract() {
        PredictionRequestDto request = request(2, 0.8f, 24, 48, 72);

        predictionService.savePrediction(request);

        verify(predictionRepository).save(argThat(saved ->
                saved.getPlant() == plant
                        && saved.getPredictedStage() == 2
                        && saved.getConfidence() == 0.8f
                        && saved.getEtaLowerHours() == 24
                        && saved.getEtaPointHours() == 48
                        && saved.getEtaUpperHours() == 72
                        && "LIGHTGBM_AI".equals(saved.getPredictionMode())
        ));
    }

    @Test
    void rejectsPredictionForAnythingButNextStage() {
        PredictionRequestDto request = request(3, 0.8f, 24, 48, 72);
        assertThrows(IllegalArgumentException.class, () -> predictionService.savePrediction(request));
        verify(predictionRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidEtaOrdering() {
        PredictionRequestDto request = request(2, 0.8f, 72, 48, 24);
        assertThrows(IllegalArgumentException.class, () -> predictionService.savePrediction(request));
        verify(predictionRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidConfidence() {
        PredictionRequestDto request = request(2, Float.NaN, 24, 48, 72);
        assertThrows(IllegalArgumentException.class, () -> predictionService.savePrediction(request));
        verify(predictionRepository, never()).save(any());
    }

    private PredictionRequestDto request(
            int stage,
            float confidence,
            int lower,
            int point,
            int upper
    ) {
        PredictionRequestDto request = new PredictionRequestDto();
        ReflectionTestUtils.setField(request, "plant_id", 1L);
        ReflectionTestUtils.setField(request, "predicted_stage", stage);
        ReflectionTestUtils.setField(request, "confidence", confidence);
        ReflectionTestUtils.setField(request, "prediction_mode", "LIGHTGBM_AI");
        ReflectionTestUtils.setField(request, "eta_lower_hours", lower);
        ReflectionTestUtils.setField(request, "eta_point_hours", point);
        ReflectionTestUtils.setField(request, "eta_upper_hours", upper);
        return request;
    }
}
