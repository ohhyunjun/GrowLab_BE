package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.service.SharedEnvironment;
import com.metaverse.growlab_be.device.dto.PortStateSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttPublisher {

    private final MessageChannel mqttOutboundChannel;
    private static final ObjectMapper PORT_STATE_JSON = new ObjectMapper();

    /** Snapshot values are captured now; rolled-back transactions must never reach the RPi. */
    public void publishPortState(String serial, String portStatus, List<Plant> plants) {
        String topic = "growlab/" + serial + "/ports";
        final String payload;
        try {
            payload = PORT_STATE_JSON.writeValueAsString(PortStateSnapshot.from(serial, portStatus, plants));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("포트 상태를 직렬화할 수 없습니다.", e);
        }
        Runnable publish = () -> {
            try {
                send(topic, payload, true);
                log.info("[MQTT] 포트 상태 전송(retained) → topic={}", topic);
            } catch (RuntimeException e) {
                // DB has committed. Periodic refresh republishes the authoritative state.
                log.warn("[MQTT] 포트 상태 전송 실패, 주기적 재동기화 예정 - serial={}", serial, e);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }

    public void publishCommand(String deviceSerial, String cmd) {
        String topic = "growlab/" + deviceSerial + "/command";
        send(topic, cmd, true);
        log.info("[MQTT] 명령 전송(retained) → topic={} cmd={}", topic, cmd);
    }

    public void publishPhotoInterval(String deviceSerial, Integer hours) {
        String topic = "growlab/" + deviceSerial + "/photo_interval";
        send(topic, String.valueOf(hours), true);
        log.info("[MQTT] photoInterval 전송(retained) → topic={} hours={}", topic, hours);
    }

    public void publishThresholds(String deviceSerial, Species species) {
        String topic = "growlab/" + deviceSerial + "/thresholds";
        String payload = String.join(",",
                formatThreshold(species == null ? null : species.getMinTemperature()),
                formatThreshold(species == null ? null : species.getMaxTemperature()),
                formatThreshold(species == null ? null : species.getMinHumidity()),
                formatThreshold(species == null ? null : species.getMaxHumidity()),
                formatThreshold(species == null ? null : species.getMinPh()),
                formatThreshold(species == null ? null : species.getMaxPh()),
                formatThreshold(species == null ? null : species.getMinTds()),
                formatThreshold(species == null ? null : species.getMaxTds())
        );

        send(topic, payload, true);
        log.info("[MQTT] thresholds 전송(retained) → topic={} payload={}", topic, payload);
    }

    // Arduino receives one range per device: the intersection of all planted species.
    public void publishThresholdsForPlants(String deviceSerial, List<Plant> plants) {
        if (plants == null || plants.isEmpty()) {
            publishThresholds(deviceSerial, null);
            return;
        }
        try {
            SharedEnvironment common = SharedEnvironment.fromSpecies(
                    plants.stream().map(Plant::getSpecies).toList());
            String payload = String.join(",",
                    formatThreshold(common.temperature().min()), formatThreshold(common.temperature().max()),
                    formatThreshold(common.humidity().min()), formatThreshold(common.humidity().max()),
                    formatThreshold(common.ph().min()), formatThreshold(common.ph().max()),
                    formatThreshold(common.tds().min()), formatThreshold(common.tds().max()));
            String topic = "growlab/" + deviceSerial + "/thresholds";
            send(topic, payload, true);
            log.info("[MQTT] 공통 thresholds 전송(retained) → topic={} payload={}", topic, payload);
        } catch (IllegalArgumentException | IllegalStateException error) {
            log.warn("[MQTT] 공통 범위를 계산할 수 없어 임계값을 비움 - serial={}", deviceSerial, error);
            publishThresholds(deviceSerial, null);
        }
    }

    private String formatThreshold(Double value) {
        return value == null ? "NA" : String.format(Locale.ROOT, "%.1f", value);
    }

    private void send(String topic, String payload, boolean retained) {
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .setHeader(MqttHeaders.RETAINED, retained)
                        .build()
        );
    }
}
