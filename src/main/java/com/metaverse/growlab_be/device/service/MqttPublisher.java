package com.metaverse.growlab_be.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttPublisher {

    private final MessageChannel mqttOutboundChannel;

    public void publishCommand(String deviceSerial, String cmd) {
        String topic = "growlab/" + deviceSerial + "/command";
        send(topic, cmd);
        log.info("[MQTT] 명령 전송 → topic={} cmd={}", topic, cmd);
    }

    public void publishPhotoInterval(String deviceSerial, Integer hours) {
        String topic = "growlab/" + deviceSerial + "/photo_interval";
        send(topic, String.valueOf(hours));
        log.info("[MQTT] photoInterval 전송 → topic={} hours={}", topic, hours);
    }

    private void send(String topic, String payload) {
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build()
        );
    }
}