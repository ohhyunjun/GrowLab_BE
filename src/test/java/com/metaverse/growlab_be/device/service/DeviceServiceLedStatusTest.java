package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceServiceLedStatusTest {

    private static final String SERIAL = "GROWLAB-G111";

    private final SensorLogService sensorLogService = mock(SensorLogService.class);
    @SuppressWarnings("unchecked")
    private final DeviceService service = new DeviceService(
            mock(DeviceRepository.class), mock(PhotoRepository.class), mock(SensorLogRepository.class),
            mock(NoticeRepository.class), mock(DiaryRepository.class), mock(ObjectProvider.class),
            sensorLogService);

    @BeforeEach
    void noRealtimeReportYet() {
        // Mockito는 Boolean 반환을 false로 채우므로 "보고 없음"을 명시한다.
        when(sensorLogService.getReportedLedStatus(SERIAL)).thenReturn(null);
    }

    @Test
    void 기기가_보고한_실제_상태가_저장된_명령보다_우선한다() {
        Device device = autoDevice(LocalTime.of(0, 0), LocalTime.of(18, 0));
        device.setLedStatus(true);
        when(sensorLogService.getReportedLedStatus(SERIAL)).thenReturn(false);

        assertThat(service.currentLedStatus(device, LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void 보고가_없는_자동_모드는_일정으로_판단한다() {
        Device device = autoDevice(LocalTime.of(6, 0), LocalTime.of(22, 0));
        device.setLedStatus(false);

        assertThat(service.currentLedStatus(device, LocalTime.of(6, 0))).isTrue();
        assertThat(service.currentLedStatus(device, LocalTime.of(21, 59))).isTrue();
        assertThat(service.currentLedStatus(device, LocalTime.of(22, 0))).isFalse();
        assertThat(service.currentLedStatus(device, LocalTime.of(5, 59))).isFalse();
    }

    @Test
    void 자정을_넘는_일정도_판단한다() {
        Device device = autoDevice(LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertThat(service.currentLedStatus(device, LocalTime.of(23, 0))).isTrue();
        assertThat(service.currentLedStatus(device, LocalTime.of(3, 0))).isTrue();
        assertThat(service.currentLedStatus(device, LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void 시작과_종료가_같은_일정은_꺼짐이다() {
        Device device = autoDevice(LocalTime.of(8, 0), LocalTime.of(8, 0));

        assertThat(service.currentLedStatus(device, LocalTime.of(8, 0))).isFalse();
    }

    @Test
    void 보고가_없는_수동_모드는_마지막_명령을_보여준다() {
        Device device = new Device(SERIAL, "test");
        device.setLedMode(false);
        device.setLedStatus(true);

        assertThat(service.currentLedStatus(device, LocalTime.of(3, 0))).isTrue();
    }

    private Device autoDevice(LocalTime on, LocalTime off) {
        Device device = new Device(SERIAL, "test");
        device.setLedMode(true);
        device.setLedOnTime(on);
        device.setLedOffTime(off);
        return device;
    }
}
