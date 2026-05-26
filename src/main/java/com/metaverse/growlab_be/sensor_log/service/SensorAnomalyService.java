package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.sensor_log.domain.SensorAnomaly;
import com.metaverse.growlab_be.sensor_log.domain.SensorType;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogAnomalyRequestDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorAnomalyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorAnomalyService {

    private final SensorAnomalyRepository anomalyRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public void startAnomaly(SensorLogAnomalyRequestDto dto) {
        Device device = getValidDeviceById(dto.getSerial_number());
        SensorType sensorType = SensorType.valueOf(dto.getSensor_type().toUpperCase());

        boolean alreadyActive = anomalyRepository
                .findTopByDeviceAndSensorTypeAndEndedAtIsNull(device, sensorType)
                .isPresent();

        if (alreadyActive) return;

        anomalyRepository.save(new SensorAnomaly(device, sensorType, dto.getValue()));
    }

    @Transactional
    public void endAnomaly(SensorLogAnomalyRequestDto dto) {
        Device device = getValidDeviceById(dto.getSerial_number());
        SensorType sensorType = SensorType.valueOf(dto.getSensor_type().toUpperCase());

        anomalyRepository
                .findTopByDeviceAndSensorTypeAndEndedAtIsNull(device, sensorType)
                .ifPresent(SensorAnomaly::close);
    }

    private Device getValidDeviceById(String serialNumber) {
        return deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
    }
}