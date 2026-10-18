package com.metaverse.growlab_be.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantEndReason;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.plant.service.PlantService;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortStateLifecycleTest {
    private final List<Message<?>> sent = new ArrayList<>();
    private final MessageChannel channel = (message, timeout) -> { sent.add(message); return true; };
    private final MqttPublisher publisher = new MqttPublisher(channel);
    private final ObjectMapper json = new ObjectMapper();

    private List<JsonNode> snapshots() throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (Message<?> message : sent) {
            if (String.valueOf(message.getHeaders().get(MqttHeaders.TOPIC)).endsWith("/ports")) {
                assertThat(message.getHeaders().get(MqttHeaders.RETAINED)).isEqualTo(true);
                result.add(json.readTree(message.getPayload().toString()));
            }
        }
        return result;
    }

    @Test
    void harvestOffThenNewPlantOnUsesNewIdentity() throws Exception { replace(PlantEndReason.HARVESTED); }

    @Test
    void deathOffThenNewPlantOnUsesNewIdentity() throws Exception { replace(PlantEndReason.DIED); }

    private void replace(PlantEndReason reason) throws Exception {
        User owner = new User(); owner.setId(5L);
        Device device = new Device("TEST-PORTS", "test");
        device.setUser(owner); device.setPortStatus("01000000");
        Species species = new Species(); species.setId(1L); species.setName("상추");
        Plant old = new Plant(); old.setId(7L); old.setDevice(device); old.setSpecies(species);
        old.setPortIndex(1); old.setActivePortIndex(1); old.setName("old");
        device.getPlants().add(old);
        PlantRepository plants = mock(PlantRepository.class);
        DeviceRepository devices = mock(DeviceRepository.class);
        SpeciesRepository speciesRepo = mock(SpeciesRepository.class);
        when(plants.findByIdAndUserId(7L, 5L)).thenReturn(Optional.of(old));
        when(plants.findByDeviceIdAndEndedAtIsNull(device.getId())).thenAnswer(i -> device.getActivePlants());
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        when(speciesRepo.findById(1L)).thenReturn(Optional.of(species));
        when(plants.saveAndFlush(any(Plant.class))).thenAnswer(i -> {
            Plant p = i.getArgument(0); p.setId(8L); device.getPlants().add(p); return p;
        });
        StaticListableBeanFactory beans = new StaticListableBeanFactory(); beans.addBean("mqtt", publisher);
        var provider = beans.getBeanProvider(MqttPublisher.class);
        PlantService plantService = new PlantService(plants, mock(DiaryRepository.class), devices,
                speciesRepo, mock(NoticeService.class), provider);
        DeviceService deviceService = new DeviceService(devices, mock(PhotoRepository.class),
                mock(SensorLogRepository.class), mock(NoticeRepository.class), mock(DiaryRepository.class), provider, mock(com.metaverse.growlab_be.sensor_log.service.SensorLogService.class));

        plantService.finishPlant(7L, reason, owner);
        PlantRequestDto request = new PlantRequestDto();
        ReflectionTestUtils.setField(request, "name", "new");
        ReflectionTestUtils.setField(request, "serialNumber", device.getId());
        ReflectionTestUtils.setField(request, "portIndex", 1);
        ReflectionTestUtils.setField(request, "speciesId", 1L);
        ReflectionTestUtils.setField(request, "plantedAt", LocalDateTime.of(2026, 10, 18, 12, 0));
        plantService.createPlant(request, owner);
        deviceService.updatePortStatus(device.getId(), 1, true, owner);

        List<JsonNode> messages = snapshots();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).get("ports").get(1).get("active").asBoolean()).isFalse();
        assertThat(messages.get(0).get("ports").get(1).get("plantId").isNull()).isTrue();
        assertThat(messages.get(1).get("ports").get(1).get("active").asBoolean()).isFalse();
        assertThat(messages.get(1).get("ports").get(1).get("plantId").asLong()).isEqualTo(8L);
        JsonNode on = messages.get(2);
        assertThat(on.get("ports").size()).isEqualTo(8);
        assertThat(on.get("portStatus").asText()).isEqualTo("01000000");
        assertThat(on.get("ports").get(1).get("active").asBoolean()).isTrue();
        assertThat(on.get("ports").get(1).get("plantId").asLong()).isEqualTo(8L);
        assertThat(on.get("ports").get(1).get("speciesId").asLong()).isEqualTo(1L);
        assertThat(old.getEndReason()).isEqualTo(reason);
    }

    @Test
    void noPlantMeansNoCaptureEvenIfPortBitIsOn() throws Exception {
        publisher.publishPortState("TEST", "11111111", List.of());
        assertThat(snapshots().get(0).get("portStatus").asText()).isEqualTo("00000000");
    }

    @Test
    void snapshotIsPublishedOnlyAfterCommitAndCapturesValues() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.publishPortState("TEST", "00000000", List.of());
            assertThat(sent).isEmpty();
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) callback.afterCommit();
            assertThat(snapshots()).hasSize(1);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void rolledBackChangesAreNotSent() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.publishPortState("TEST", "00000000", List.of());
            for (var callback : TransactionSynchronizationManager.getSynchronizations())
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            assertThat(sent).isEmpty();
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void refreshRepublishesCurrentDatabaseState() throws Exception {
        DeviceRepository devices = mock(DeviceRepository.class);
        Device device = new Device("TEST", "test");
        when(devices.findAll()).thenReturn(List.of(device));
        new PortStateRefresh(devices, publisher).refresh();
        assertThat(snapshots().get(0).get("portStatus").asText()).isEqualTo("00000000");
    }

    @Test
    void failedSendCanRecoverOnNextRefresh() throws Exception {
        MessageChannel failing = mock(MessageChannel.class);
        when(failing.send(any(Message.class))).thenReturn(false, true);
        MqttPublisher retrying = new MqttPublisher(failing);
        DeviceRepository devices = mock(DeviceRepository.class);
        when(devices.findAll()).thenReturn(List.of(new Device("TEST", "test")));
        PortStateRefresh refresh = new PortStateRefresh(devices, retrying);
        refresh.refresh(); refresh.refresh();
        verify(failing, times(2)).send(any(Message.class));
    }
}
