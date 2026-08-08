package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SensorLogService {
    private final SensorLogRepository sensorLogRepository;
    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    @Value("${inference.server.url:http://localhost:5000}")
    private String inferenceServerUrl;

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, SensorLogRequestDto> latestDataMap = new ConcurrentHashMap<>();

    @Transactional
    public SensorLogResponseDto createSensorLog (SensorLogRequestDto sensorLogRequestDto) {
        Device device = getValidDeviceById(sensorLogRequestDto.getSerial_number());

        SensorLog sensorLog = new SensorLog(
                device,
                sensorLogRequestDto.getTemperature(),
                sensorLogRequestDto.getHumidity(),
                sensorLogRequestDto.getPh(),
                sensorLogRequestDto.getTds(),
                sensorLogRequestDto.getWater_level_status()
        );
        SensorLog savedLog = sensorLogRepository.save(sensorLog);
        triggerInference(device);

        return new SensorLogResponseDto(savedLog);
    }

    public void pushRealtime(SensorLogRequestDto dto) {
        latestDataMap.put(dto.getSerial_number(), dto);
        SseEmitter emitter = emitterMap.get(dto.getSerial_number());
        if (emitter == null) return;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("serial_number",      dto.getSerial_number());
            payload.put("temperature",        dto.getTemperature());
            payload.put("humidity",           dto.getHumidity());
            payload.put("ph",                 dto.getPh());
            payload.put("tds",                dto.getTds());
            payload.put("water_level_status", dto.getWater_level_status());

            emitter.send(SseEmitter.event()
                    .name("sensor")
                    .data(payload));
        } catch (IOException e) {
            emitterMap.remove(dto.getSerial_number());
        }
    }

    public SseEmitter createEmitter(String serialNumber) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        emitterMap.put(serialNumber, emitter);
        emitter.onCompletion(() -> emitterMap.remove(serialNumber));
        emitter.onTimeout(()    -> emitterMap.remove(serialNumber));
        emitter.onError((e)     -> emitterMap.remove(serialNumber));

        SensorLogRequestDto latest = latestDataMap.get(serialNumber);
        try {
            if (latest != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("serial_number",      latest.getSerial_number());
                payload.put("temperature",        latest.getTemperature());
                payload.put("humidity",           latest.getHumidity());
                payload.put("ph",                 latest.getPh());
                payload.put("tds",                latest.getTds());
                payload.put("water_level_status", latest.getWater_level_status());
                emitter.send(SseEmitter.event().name("sensor").data(payload));
            } else {
                emitter.send(SseEmitter.event().name("connect").data("connected"));
            }
        } catch (IOException e) {
            emitterMap.remove(serialNumber);
        }

        return emitter;
    }

    // ✅ current_stage를 stageIndex로 교체, 품종 정보(species_id/name, total_stages)를 함께 전송
    // → 추론 서버가 어떤 품종의 몇 단계 기준인지 알 수 있도록 함
    private void triggerInference(Device device) {
        if (device.getPlants() == null || device.getPlants().isEmpty()) return;

        Species species = device.getSpecies();

        for (Plant plant : device.getPlants()) {
            try {
                int daysFromStart = (int) ChronoUnit.DAYS.between(
                        plant.getPlantedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                );

                Map<String, Object> request = new HashMap<>();
                request.put("plant_id",        plant.getId());
                request.put("serial_number",   device.getId());
                request.put("current_stage",   plant.getStageIndex());
                request.put("days_from_start", daysFromStart);

                // ✅ 품종별로 다른 단계 수/모델을 구분하기 위한 정보 추가
                if (species != null) {
                    request.put("species_id",    species.getId());
                    request.put("species_name",  species.getName());
                    request.put("total_stages",  species.getStageCount());
                }

                restTemplate.postForEntity(
                        inferenceServerUrl + "/predict",
                        request,
                        Void.class
                );
            } catch (Exception e) {
            }
        }
    }

    public Device getValidDeviceById(String serialNumber){
        return deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
    }

    public SensorLogRequestDto getLatestData(String serialNumber) {
        return latestDataMap.get(serialNumber);
    }
}