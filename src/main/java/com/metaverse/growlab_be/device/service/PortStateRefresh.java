package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Recover a lost publish or a broker restart from current committed DB state. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class PortStateRefresh {
    private final DeviceRepository devices;
    private final MqttPublisher publisher;

    @Scheduled(fixedDelayString = "${mqtt.port-state-refresh-ms:60000}",
            initialDelayString = "${mqtt.port-state-refresh-ms:60000}")
    @Transactional(readOnly = true)
    public void refresh() {
        for (Device device : devices.findAll()) {
            try {
                publisher.publishPortState(device.getId(), device.getPortStatus(), device.getActivePlants());
            } catch (RuntimeException e) {
                log.warn("[MQTT] 포트 상태 재동기화 실패 - serial={}", device.getId(), e);
            }
        }
    }
}
