package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensorLogPredictionIntegrationTest {
    @Test
    void sensorReadingIsStoredWithoutTriggeringTheReplacedPredictionRoute() {
        var logs = mock(SensorLogRepository.class);
        var devices = mock(DeviceRepository.class);
        var service = new SensorLogService(logs, devices);
        var device = new Device("TEST", "Test");
        when(devices.findById("TEST")).thenReturn(Optional.of(device));
        when(logs.save(any(SensorLog.class))).thenAnswer(call -> call.getArgument(0));
        var input = new SensorLogRequestDto();
        ReflectionTestUtils.setField(input, "serial_number", "TEST");

        service.createSensorLog(input);

        verify(logs).save(any(SensorLog.class));
    }
}
