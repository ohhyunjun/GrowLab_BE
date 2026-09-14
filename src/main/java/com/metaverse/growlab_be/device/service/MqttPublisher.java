package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttPublisher {

    private final MessageChannel mqttOutboundChannel;

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
