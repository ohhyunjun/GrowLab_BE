package com.metaverse.growlab_be.sensor_log.controller;

import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
