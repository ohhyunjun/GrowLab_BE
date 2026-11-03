package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.controller.DeviceController;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.LedRequestDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeviceSettingsSaveTest {
    private static final String SERIAL = "GROWLAB-SAVE-TEST";
    private final DeviceRepository devices = mock(DeviceRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MqttPublisher> publishers = mock(ObjectProvider.class);
    private final MqttPublisher publisher = mock(MqttPublisher.class);
    private final User owner = new User();
    private final Device device = new Device(SERIAL, "test");
    private final DeviceService service = new DeviceService(devices, mock(PhotoRepository.class),
            mock(SensorLogRepository.class), mock(NoticeRepository.class), mock(DiaryRepository.class),
            publishers, mock(SensorLogService.class));
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        owner.setId(7L);
        device.setUser(owner);
        when(devices.findById(SERIAL)).thenReturn(Optional.of(device));
        PrincipalDetails principal = mock(PrincipalDetails.class);
        when(principal.getUser()).thenReturn(owner);
        mvc = MockMvcBuilders.standaloneSetup(new DeviceController(service))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(PrincipalDetails.class);
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer model,
                            NativeWebRequest request, WebDataBinderFactory binder) {
                        return principal;
                    }
                }).build();
    }

    @Test
    void disabledMqttSavesCaptureIntervalAndReturnsDbOnlyHttpSuccess() throws Exception {
        mvc.perform(patch("/api/devices/{serial}/photo_interval", SERIAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"photoInterval\":2}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2시간 촬영 주기 DB 저장 완료")))
                .andExpect(content().string(containsString("기기 전송 안 됨")));
        assertThat(device.getPhotoInterval()).isEqualTo(2);
        verifyNoInteractions(publisher);
    }

    @Test
    void disabledMqttSavesManualLedStateAndMode() throws Exception {
        device.setLedMode(true);
        mvc.perform(patch("/api/devices/{serial}/led", SERIAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ledMode\":false,\"ledStatus\":true}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LED 설정 DB 저장 완료")));
        assertThat(device.getLedMode()).isFalse();
        assertThat(device.getLedStatus()).isTrue();
    }

    @Test
    void disabledMqttSavesAutomaticScheduleWithRealJsonTimeBinding() throws Exception {
        mvc.perform(patch("/api/devices/{serial}/led", SERIAL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ledMode\":true,\"ledOnTime\":\"08:30\",\"ledOffTime\":\"20:30\"}"))
                .andExpect(status().isOk());
        assertThat(device.getLedMode()).isTrue();
        assertThat(device.getLedOnTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(device.getLedOffTime()).isEqualTo(LocalTime.of(20, 30));
    }

    @Test
    void enabledMqttStillPublishesCaptureSetting() throws Exception {
        when(publishers.getIfAvailable()).thenReturn(publisher);
        mvc.perform(patch("/api/devices/{serial}/photo_interval", SERIAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"photoInterval\":3}"))
                .andExpect(status().isOk())
                .andExpect(content().string("3시간마다 촬영으로 설정되었습니다."));
        verify(publisher).publishPhotoInterval(SERIAL, 3);
    }

    @Test
    void enabledMqttStillPublishesManualAndAutomaticCommands() {
        when(publishers.getIfAvailable()).thenReturn(publisher);
        LedRequestDto manual = mock(LedRequestDto.class);
        when(manual.getLedStatus()).thenReturn(false);
        assertThat(service.controlLed(SERIAL, manual, owner)).isTrue();
        verify(publisher).publishCommand(SERIAL, "o");

        LedRequestDto auto = mock(LedRequestDto.class);
        when(auto.getLedMode()).thenReturn(true);
        when(auto.getLedOnTime()).thenReturn(LocalTime.of(8, 30));
        when(auto.getLedOffTime()).thenReturn(LocalTime.of(20, 30));
        assertThat(service.controlLed(SERIAL, auto, owner)).isTrue();
        verify(publisher).publishCommand(SERIAL, "SCHED:08:30-20:30");
    }

    @Test
    void enabledButBrokenBrokerDoesNotReportDbOnlySuccess() throws Exception {
        when(publishers.getIfAvailable()).thenReturn(publisher);
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publishPhotoInterval(SERIAL, 2);
        mvc.perform(patch("/api/devices/{serial}/photo_interval", SERIAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"photoInterval\":2}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("MQTT 브로커에 촬영 주기 명령을 보낼 수 없습니다."));
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publishCommand(SERIAL, "O");
        mvc.perform(patch("/api/devices/{serial}/led", SERIAL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ledMode\":false,\"ledStatus\":true}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void captureIntervalCannotBeMissingNullOrOutsideOneToSix() throws Exception {
        for (String json : new String[]{"{}", "{\"photoInterval\":null}", "{\"photoInterval\":0}", "{\"photoInterval\":7}"}) {
            mvc.perform(patch("/api/devices/{serial}/photo_interval", SERIAL)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        for (Integer invalid : new Integer[]{null, 0, 7}) {
            assertThatThrownBy(() -> service.updatePhotoInterval(SERIAL, invalid, owner))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(device.getPhotoInterval()).isEqualTo(6);
    }

    @Test
    void disabledMqttDoesNotBypassOwnershipOrLedValidation() throws Exception {
        User other = new User();
        other.setId(99L);
        assertThatThrownBy(() -> service.updatePhotoInterval(SERIAL, 2, other))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.controlLed(SERIAL, mock(LedRequestDto.class), other))
                .isInstanceOf(AccessDeniedException.class);
        for (String json : new String[]{"{\"ledMode\":false}", "{\"ledMode\":true}"}) {
            mvc.perform(patch("/api/devices/{serial}/led", SERIAL).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isNotFound());
        }
        assertThat(device.getPhotoInterval()).isEqualTo(6);
        assertThat(device.getLedStatus()).isFalse();
    }
}
