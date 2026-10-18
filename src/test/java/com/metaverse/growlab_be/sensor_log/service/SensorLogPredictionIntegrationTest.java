package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.prediction.dto.PredictionRequestDto;
import com.metaverse.growlab_be.prediction.service.PredictionService;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensorLogPredictionIntegrationTest {

    @Test void lettuceSensorLogDoesNotCallLegacyInference() {
        var logs=mock(SensorLogRepository.class);var devices=mock(DeviceRepository.class);
        var client=mock(RestTemplate.class);var predictions=mock(PredictionService.class);
        var service=new SensorLogService(logs,devices,client,predictions);
        var species=new Species();species.setName("청상추");
        var device=new Device("TEST","Test");var plant=new Plant();plant.setSpecies(species);device.getPlants().add(plant);
        when(devices.findById("TEST")).thenReturn(Optional.of(device));
        when(logs.save(any(SensorLog.class))).thenAnswer(i->i.getArgument(0));
        var dto=new SensorLogRequestDto();ReflectionTestUtils.setField(dto,"serial_number","TEST");
        service.createSensorLog(dto);
        org.mockito.Mockito.verifyNoInteractions(client,predictions);
    }

    @Test
    @SuppressWarnings("unchecked")
    void hourlySensorLogCallsInferenceWithSpeciesContractAndSavesResponse() {
        SensorLogRepository sensorLogRepository = mock(SensorLogRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        PredictionService predictionService = mock(PredictionService.class);

        SensorLogService service = new SensorLogService(
                sensorLogRepository,
                deviceRepository,
                restTemplate,
                predictionService
        );
        ReflectionTestUtils.setField(service, "inferenceServerUrl", "http://inference:5000");

        Species species = new Species();
        species.setId(7L);
        species.setName("딸기"); // Other crops retain the legacy sensor inference contract.
        species.setStageNames(List.of("씨앗", "정식기", "생육기", "수확기"));
        species.setStageDurationDays(List.of(0, 7, 20, 40));

        Device device = new Device("GROWLAB-G111", "테스트 기기");
        Plant plant = new Plant();
        plant.setSpecies(species);
        plant.setId(10L);
        plant.setDevice(device);
        plant.setStageIndex(1);
        plant.setPlantedAt(LocalDateTime.now().minusDays(8));
        device.getPlants().add(plant);

        SensorLogRequestDto sensor = new SensorLogRequestDto();
        ReflectionTestUtils.setField(sensor, "serial_number", device.getId());
        ReflectionTestUtils.setField(sensor, "temperature", 23.5f);
        ReflectionTestUtils.setField(sensor, "humidity", 65.0f);
        ReflectionTestUtils.setField(sensor, "ph", 6.1f);
        ReflectionTestUtils.setField(sensor, "tds", 700.0f);
        ReflectionTestUtils.setField(sensor, "water_level_status", true);

        PredictionRequestDto prediction = new PredictionRequestDto();
        ReflectionTestUtils.setField(prediction, "plant_id", 10L);
        ReflectionTestUtils.setField(prediction, "predicted_stage", 2);
        ReflectionTestUtils.setField(prediction, "confidence", 0.8f);
        ReflectionTestUtils.setField(prediction, "prediction_mode", "LIGHTGBM_AI");
        ReflectionTestUtils.setField(prediction, "eta_lower_hours", 24);
        ReflectionTestUtils.setField(prediction, "eta_point_hours", 48);
        ReflectionTestUtils.setField(prediction, "eta_upper_hours", 72);

        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(sensorLogRepository.save(any(SensorLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(
                eq("http://inference:5000/predict"),
                any(Map.class),
                eq(PredictionRequestDto.class)
        )).thenReturn(ResponseEntity.ok(prediction));

        service.createSensorLog(sensor);

        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForEntity(
                eq("http://inference:5000/predict"),
                requestCaptor.capture(),
                eq(PredictionRequestDto.class)
        );
        Map<String, Object> request = requestCaptor.getValue();
        assertThat(request)
                .containsEntry("plant_id", 10L)
                .containsEntry("serial_number", "GROWLAB-G111")
                .containsEntry("current_stage", 1)
                .containsEntry("species_id", 7L)
                .containsEntry("species_name", "딸기")
                .containsEntry("total_stages", 4)
                .containsEntry("temperature", 23.5f)
                .containsEntry("humidity", 65.0f)
                .containsEntry("ph", 6.1f)
                .containsEntry("tds", 700.0f);
        verify(predictionService).savePrediction(prediction);
    }
}
