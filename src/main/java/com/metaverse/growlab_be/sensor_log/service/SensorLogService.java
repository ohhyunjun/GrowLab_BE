package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
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

    // SSE emitter 저장소: serialNumber → SseEmitter
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, SensorLogRequestDto> latestDataMap = new ConcurrentHashMap<>();

    @Transactional
    public SensorLogResponseDto createSensorLog (SensorLogRequestDto sensorLogRequestDto) {
        // 시리얼 번호로 DB에서 기기 찾기
        Device device = getValidDeviceById(sensorLogRequestDto.getSerial_number());

        // sensorlog 엔터티 생성
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

    // 추가: RPi 실시간 수신 → SSE로 프론트 push (DB 저장 X)
    public void pushRealtime(SensorLogRequestDto dto) {
        latestDataMap.put(dto.getSerial_number(), dto);
        SseEmitter emitter = emitterMap.get(dto.getSerial_number());
        if (emitter == null) return;

        try {
            // Map.of() → HashMap으로 변경 (null 허용)
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

    // 추가: 프론트 SSE 연결 수립 (프론트에서 자동 재연결)
    public SseEmitter createEmitter(String serialNumber) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분 그대로

        emitterMap.put(serialNumber, emitter);
        emitter.onCompletion(() -> emitterMap.remove(serialNumber));
        emitter.onTimeout(()    -> emitterMap.remove(serialNumber));
        emitter.onError((e)     -> emitterMap.remove(serialNumber));

        SensorLogRequestDto latest = latestDataMap.get(serialNumber);
        try {
            if (latest != null) {
                Map<String, Object> payload = new HashMap<>();  // Map.of() → HashMap
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

    private void triggerInference(Device device) {
        if (device.getPlants() == null || device.getPlants().isEmpty()) return;

        for (Plant plant : device.getPlants()) {
            try {
                int daysFromStart = (int) ChronoUnit.DAYS.between(
                        plant.getPlantedAt().toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                );

                Map<String, Object> request = new HashMap<>();
                request.put("plant_id",        plant.getId());
                request.put("serial_number",   device.getId());
                request.put("current_stage",   plant.getPlantStage().ordinal());
                request.put("days_from_start", daysFromStart);

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

}
