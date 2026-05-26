package com.metaverse.growlab_be.sensor_log.controller;

import com.metaverse.growlab_be.sensor_log.dto.SensorLogAnomalyRequestDto;
import com.metaverse.growlab_be.sensor_log.service.SensorAnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class SensorAnomalyController {
    private final SensorAnomalyService anomalyService;

    @PostMapping
    public ResponseEntity<Void> startAnomaly(@RequestBody SensorLogAnomalyRequestDto dto){
        anomalyService.startAnomaly(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    public ResponseEntity<Void> endAnomaly(@RequestBody SensorLogAnomalyRequestDto dto) {
        anomalyService.endAnomaly(dto);
        return ResponseEntity.ok().build();
    }

}
