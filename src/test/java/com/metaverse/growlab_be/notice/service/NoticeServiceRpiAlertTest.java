package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.notice.domain.Notice;
import com.metaverse.growlab_be.notice.dto.NoticeRequestDto;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeServiceRpiAlertTest {

    @Test
    void storesOneArduinoAlertWithAffectedSpecies() {
        DeviceRepository devices = mock(DeviceRepository.class);
        NoticeRepository notices = mock(NoticeRepository.class);
        NoticeService service = new NoticeService(notices, mock(UserRepository.class), devices);
        Device device = mixedDevice();
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));

        NoticeRequestDto dto = new NoticeRequestDto();
        ReflectionTestUtils.setField(dto, "serial_number", device.getId());
        ReflectionTestUtils.setField(dto, "sensor", "TEMP");
        ReflectionTestUtils.setField(dto, "value", 28.0f);
        service.createAlertFromRpi(dto);

        ArgumentCaptor<Notice> saved = ArgumentCaptor.forClass(Notice.class);
        verify(notices).save(saved.capture());
        assertThat(saved.getValue().getMessage()).contains("딸기").doesNotContain("상추");
        assertThat(saved.getValue().getAdditionalData())
                .contains("\"source\":\"arduino_alert\"")
                .contains("\"sensor\":\"TEMP\"");
    }

    @Test
    void ignoresStaleArduinoAlertAfterSharedRangeChanges() {
        DeviceRepository devices = mock(DeviceRepository.class);
        NoticeRepository notices = mock(NoticeRepository.class);
        NoticeService service = new NoticeService(notices, mock(UserRepository.class), devices);
        Device device = mixedDevice();
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));

        NoticeRequestDto dto = new NoticeRequestDto();
        ReflectionTestUtils.setField(dto, "serial_number", device.getId());
        ReflectionTestUtils.setField(dto, "sensor", "TEMP");
        ReflectionTestUtils.setField(dto, "value", 24.0f);
        service.createAlertFromRpi(dto);

        verify(notices, never()).save(any(Notice.class));
    }

    private Device mixedDevice() {
        Device device = new Device("GROWLAB-G111", "테스트 기기");
        device.setUser(new User());
        Species strawberry = new Species();
        strawberry.setName("딸기");
        strawberry.setMinTemperature(18.0);
        strawberry.setMaxTemperature(25.0);
        Species lettuce = new Species();
        lettuce.setName("상추");
        lettuce.setMinTemperature(18.0);
        lettuce.setMaxTemperature(30.0);
        Plant first = new Plant();
        first.setSpecies(strawberry);
        Plant second = new Plant();
        second.setSpecies(lettuce);
        device.getPlants().add(first);
        device.getPlants().add(second);
        return device;
    }
}
