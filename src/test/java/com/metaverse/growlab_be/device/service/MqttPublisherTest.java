package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.plant.domain.Plant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class MqttPublisherTest {

    @Test
    void publishesDeviceCommandsAndPhotoInterval() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any(Message.class))).thenReturn(true);
        MqttPublisher publisher = new MqttPublisher(channel);

        publisher.publishCommand("GROWLAB-G111", "O");
        publisher.publishPhotoInterval("GROWLAB-G111", 24);

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(channel, org.mockito.Mockito.times(2)).send(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getPayload()).isEqualTo("O");
        assertThat(messageCaptor.getAllValues().get(1).getPayload()).isEqualTo("24");
        assertThat(messageCaptor.getAllValues().get(0).getHeaders().get(MqttHeaders.TOPIC))
                .isEqualTo("growlab/GROWLAB-G111/command");
        assertThat(messageCaptor.getAllValues().get(1).getHeaders().get(MqttHeaders.TOPIC))
                .isEqualTo("growlab/GROWLAB-G111/photo_interval");
        assertThat(messageCaptor.getAllValues().get(0).getHeaders().get(MqttHeaders.RETAINED))
                .isEqualTo(true);
        assertThat(messageCaptor.getAllValues().get(1).getHeaders().get(MqttHeaders.RETAINED))
                .isEqualTo(true);
    }

    @Test
    void publishesSpeciesThresholdsAsRetainedCsv() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any(Message.class))).thenReturn(true);
        MqttPublisher publisher = new MqttPublisher(channel);
        Species species = new Species();
        species.setMinTemperature(18.5);
        species.setMaxTemperature(27.0);
        species.setMinHumidity(50.0);
        species.setMaxHumidity(80.0);
        species.setMinPh(5.5);
        species.setMaxPh(6.5);
        species.setMinTds(200.0);
        species.setMaxTds(800.0);

        publisher.publishThresholds("GROWLAB-G111", species);

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(channel).send(messageCaptor.capture());
        Message<?> message = messageCaptor.getValue();
        assertThat(message.getPayload()).isEqualTo("18.5,27.0,50.0,80.0,5.5,6.5,200.0,800.0");
        assertThat(message.getHeaders().get(MqttHeaders.TOPIC)).isEqualTo("growlab/GROWLAB-G111/thresholds");
        assertThat(message.getHeaders().get(MqttHeaders.RETAINED)).isEqualTo(true);
    }

    @Test
    void mixedPortSpeciesPublishSharedIntersection() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any(Message.class))).thenReturn(true);
        MqttPublisher publisher = new MqttPublisher(channel);
        Species strawberry = new Species();
        strawberry.setId(1L);
        strawberry.setMinTemperature(18.0);
        strawberry.setMaxTemperature(26.0);
        strawberry.setMinHumidity(50.0);
        strawberry.setMaxHumidity(80.0);
        strawberry.setMinPh(5.5);
        strawberry.setMaxPh(6.5);
        strawberry.setMinTds(300.0);
        strawberry.setMaxTds(800.0);
        strawberry.setMinLightHours(8.0);
        strawberry.setMaxLightHours(14.0);
        Species lettuce = new Species();
        lettuce.setId(2L);
        lettuce.setMinTemperature(15.0);
        lettuce.setMaxTemperature(22.0);
        lettuce.setMinHumidity(60.0);
        lettuce.setMaxHumidity(75.0);
        lettuce.setMinPh(5.8);
        lettuce.setMaxPh(7.0);
        lettuce.setMinTds(400.0);
        lettuce.setMaxTds(900.0);
        lettuce.setMinLightHours(10.0);
        lettuce.setMaxLightHours(16.0);
        Plant first = new Plant();
        first.setSpecies(strawberry);
        Plant second = new Plant();
        second.setSpecies(lettuce);

        publisher.publishThresholdsForPlants("GROWLAB-G111", List.of(first, second));

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(channel).send(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("18.0,22.0,60.0,75.0,5.8,6.5,400.0,800.0");
    }

    @Test
    void publishesUnboundedThresholdsWhenSpeciesIsCleared() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any(Message.class))).thenReturn(true);
        MqttPublisher publisher = new MqttPublisher(channel);

        publisher.publishThresholds("GROWLAB-G111", null);

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(channel).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload())
                .isEqualTo("NA,NA,NA,NA,NA,NA,NA,NA");
    }
}
