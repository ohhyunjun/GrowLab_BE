package com.metaverse.growlab_be.sensor_log.controller;

import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("api/sensor_logs")
@RequiredArgsConstructor
public class SensorLogController {
    private final SensorLogService  sensorLogService;

    //1. 센서 데이터 주입
    @PostMapping
    public ResponseEntity<SensorLogResponseDto> createSensorLog(
            @RequestBody SensorLogRequestDto sensorLogRequestDto){
        SensorLogResponseDto sensorLogResponseDto = sensorLogService.createSensorLog(sensorLogRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorLogResponseDto);
    }

    //2. 실시간 프론트 전달(db 저장아님)
    @PostMapping("/realtime")
    public ResponseEntity<Void> receivedRealtime(
            @RequestBody SensorLogRequestDto sensorLogRequestDto) {
        sensorLogService.pushRealtime(sensorLogRequestDto);
        return ResponseEntity.ok().build();
    }

    //3: 프론트 → SSE 구독
    @GetMapping(value = "/stream/{serialNumber}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSensorLog(@PathVariable String serialNumber) {
        return sensorLogService.createEmitter(serialNumber);
    }


}
