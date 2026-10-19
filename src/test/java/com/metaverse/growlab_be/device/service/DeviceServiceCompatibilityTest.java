package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.LedRequestDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DeviceServiceCompatibilityTest {

    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final PhotoRepository photos = mock(PhotoRepository.class);
    private final SensorLogRepository sensors = mock(SensorLogRepository.class);
    private final NoticeRepository notices = mock(NoticeRepository.class);
    private final DiaryRepository diaries = mock(DiaryRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MqttPublisher> publishers = mock(ObjectProvider.class);
    private final DeviceService service = new DeviceService(devices, photos, sensors, notices, diaries, publishers,
            mock(SensorLogService.class));

    @Test
    void userDeviceSummaryIncludesTheObservedDisease() {
        User user = new User();
        user.setId(1L);
        Device device = new Device("GROWLAB-G111", "test");
        Plant plant = mock(Plant.class);
        when(plant.isActive()).thenReturn(true);
        when(plant.getPortIndex()).thenReturn(3);
        when(plant.getDiseaseResult()).thenReturn("leaf_spot");
        device.getPlants().add(plant);
        when(devices.findByUserId(1L)).thenReturn(List.of(device));
        when(photos.findTopByDeviceIdOrderByCreatedAtDesc(device.getId())).thenReturn(Optional.empty());

        var summary = service.getUserDevices(user).get(0).getPlants().get(0);

        assertThat(summary.getDiseaseResult()).isEqualTo("leaf_spot");
    }

    @Test
    void ledControlFailsWithoutMqttPublisher() {
        User user = new User();
        user.setId(1L);
        Device device = new Device("GROWLAB-G111", "test");
        device.setUser(user);
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        when(publishers.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.controlLed(device.getId(), mock(LedRequestDto.class), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MQTT 제어 연결이 비활성화");
        assertThat(device.getLedStatus()).isFalse();
    }

    @Test
    void newlyProvisionedDevicePublishesSixHourRetainedSetting() {
        MqttPublisher publisher = mock(MqttPublisher.class);
        when(publishers.getIfAvailable()).thenReturn(publisher);
        when(devices.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDeviceByAdmin("GROWLAB-G112");

        verify(publisher).publishPhotoInterval("GROWLAB-G112", 6);
    }
}
