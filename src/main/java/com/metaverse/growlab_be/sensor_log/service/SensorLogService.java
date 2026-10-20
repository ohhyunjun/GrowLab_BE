package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorLogService {
    private final SensorLogRepository sensorLogRepository;
    private final DeviceRepository deviceRepository;

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, SensorLogRequestDto> latestDataMap = new ConcurrentHashMap<>();
    private final Map<String, Long> latestReceivedAtMap = new ConcurrentHashMap<>();

    // 실시간 보고는 1분 주기다. 이보다 오래된 값은 현재 상태로 보지 않는다.
    private static final long REPORTED_STATE_MAX_AGE_MS = Duration.ofMinutes(5).toMillis();

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

        return new SensorLogResponseDto(savedLog);
    }

    public void pushRealtime(SensorLogRequestDto dto) {
        getValidDeviceById(dto.getSerial_number());
        latestDataMap.put(dto.getSerial_number(), dto);
        latestReceivedAtMap.put(dto.getSerial_number(), System.currentTimeMillis());
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

    public Device getValidDeviceById(String serialNumber){
        return deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
    }

    public SensorLogRequestDto getLatestData(String serialNumber) {
        return latestDataMap.get(serialNumber);
    }

    // 기기가 최근 실시간 보고에 실어 보낸 실제 LED 상태. 보고가 없거나 오래됐으면 null.
    public Boolean getReportedLedStatus(String serialNumber) {
        Long receivedAt = latestReceivedAtMap.get(serialNumber);
        if (receivedAt == null || System.currentTimeMillis() - receivedAt > REPORTED_STATE_MAX_AGE_MS) return null;
        SensorLogRequestDto latest = latestDataMap.get(serialNumber);
        return latest == null ? null : latest.getLed_status();
    }
}


