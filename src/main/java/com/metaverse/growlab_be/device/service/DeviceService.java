package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.*;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorAnomalyRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final PhotoRepository  photoRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorAnomalyRepository sensorAnomalyRepository;
    private final NoticeRepository noticeRepository;
    private final DiaryRepository diaryRepository;
    private final ObjectProvider<MqttPublisher> mqttPublisherProvider;

    // 시작 시 포트별 품종을 기준으로 retained thresholds를 다시 발행한다.
    @EventListener(ApplicationReadyEvent.class)
    public void publishRetainedThresholdsOnStartup() {
        mqttPublisherProvider.ifAvailable(publisher ->
                deviceRepository.findAll().forEach(device -> {
                    try {
                        publisher.publishThresholdsForPlants(device.getId(), device.getActivePlants());
                    } catch (RuntimeException e) {
                        log.warn("[MQTT] 시작 시 thresholds 동기화 실패 - serial={}", device.getId(), e);
                    }
                })
        );
    }

    public List<DeviceResponseDto> getUserDevices(User user) {
        List<Device> devices = deviceRepository.findByUserId(user.getId());
        return devices.stream()
                .map(device -> {
                    LocalDateTime lastPhotoAt = photoRepository
                            .findTopByDeviceIdOrderByCreatedAtDesc(device.getId())
                            .map(Photo::getCreatedAt)
                            .orElse(null);

                    List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getActivePlants().stream()
                            .map(this::buildPlantSummary)
                            .sorted(Comparator.comparingInt(DeviceResponseDto.PlantSummaryDto::getPortIndex))
                            .toList();

                    return new DeviceResponseDto(device, lastPhotoAt, plantSummaries);
                })
                .toList();
    }

    public List<AdminDeviceResponseDto> getAllDevicesForAdmin() {
        List<Device> devices = deviceRepository.findAll();
        return devices.stream()
                .sorted(Comparator.comparing(
                        Device::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(AdminDeviceResponseDto::new)
                .toList();
    }

    @Transactional
    public void registerDevice(String serialNumber, String deviceNickname, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다: " + serialNumber));

        if (device.getUser() != null) {
            throw new IllegalStateException("이미 등록된 기기입니다.");
        }

        device.setUser(user);
        if (deviceNickname != null && !deviceNickname.isBlank()) {
            device.setDeviceNickname(deviceNickname);
        }
    }

    @Transactional
    public void createDeviceByAdmin(String serialNumber) {
        if (deviceRepository.existsById(serialNumber)) {
            throw new IllegalArgumentException("이미 존재하는 시리얼 번호입니다: " + serialNumber);
        }
        deviceRepository.save(new Device(serialNumber, serialNumber));
    }

    @Transactional
    public void deleteDevice(String serialNumber, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        if (!device.getActivePlants().isEmpty()) {
            throw new IllegalStateException("재배 중인 포트를 먼저 종료해주세요.");
        }
        device.setUser(null);
        device.setPortStatus("00000000");

        // retained 메시지에 이전 품종 범위가 남지 않도록 제한 없음 값으로 초기화한다.
        mqttPublisherProvider.ifAvailable(publisher ->
                publisher.publishThresholds(serialNumber, null));
    }

    @Transactional
    public void deleteDeviceByAdmin(String serialNumber) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다: " + serialNumber));
        diaryRepository.deleteByDevice(device);
        noticeRepository.deleteByDeviceSerial(serialNumber);
        photoRepository.deleteByDevice(device);
        sensorAnomalyRepository.deleteByDevice(device);
        sensorLogRepository.deleteByDevice(device);
        deviceRepository.delete(device);
    }

    @Transactional
    public void updatePhotoInterval(String serialNumber, Integer photoInterval, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        device.setPhotoInterval(photoInterval);
        mqttPublisherProvider.ifAvailable(publisher ->
                publisher.publishPhotoInterval(serialNumber, photoInterval));
    }

    @Transactional
    public void controlLed(String serialNumber, LedRequestDto requestDto, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        String command;

        if (Boolean.TRUE.equals(requestDto.getLedMode())) {
            if (requestDto.getLedOnTime() == null || requestDto.getLedOffTime() == null) {
                throw new IllegalArgumentException("LED 스케줄 시간은 필수입니다.");
            }
            device.setLedMode(true);
            device.setLedOnTime(requestDto.getLedOnTime());
            device.setLedOffTime(requestDto.getLedOffTime());
            command = "SCHED:" + requestDto.getLedOnTime() + "-" + requestDto.getLedOffTime();
        } else {
            if (requestDto.getLedStatus() == null) {
                throw new IllegalArgumentException("수동 LED 상태는 필수입니다.");
            }
            device.setLedMode(false);
            device.setLedStatus(requestDto.getLedStatus());
            command = Boolean.TRUE.equals(requestDto.getLedStatus()) ? "O" : "o";
        }

        mqttPublisherProvider.ifAvailable(publisher ->
                publisher.publishCommand(serialNumber, command));
    }

    @Transactional
    public void updatePortStatus(String serialNumber, Integer portIndex, Boolean status, User user) {
        if (portIndex == null || portIndex < 0 || portIndex >= 8 || status == null) {
            throw new IllegalArgumentException("포트 번호는 0~7이고 상태는 필수입니다.");
        }
        Device device = findDeviceOwnedByUser(serialNumber, user);
        if (!Boolean.TRUE.equals(status) && device.getActivePlants().stream()
                .anyMatch(plant -> portIndex.equals(plant.getPortIndex()))) {
            throw new IllegalStateException("식물이 있는 포트는 재배 종료를 통해 OFF해주세요.");
        }
        char[] bits = device.getPortStatus().toCharArray();
        bits[portIndex] = status ? '1' : '0';
        device.setPortStatus(new String(bits));
    }

    // 포트에 심은 식물의 품종으로 요약 정보를 만든다.
    private DeviceResponseDto.PlantSummaryDto buildPlantSummary(com.metaverse.growlab_be.plant.domain.Plant p) {
        Species species = p.getSpecies();
        return new DeviceResponseDto.PlantSummaryDto(
                p.getId(),
                p.getName(),
                p.getPortIndex(),
                species != null ? species.getName() : null,
                p.getStageIndex(),
                species != null ? species.getStageName(p.getStageIndex()) : null,
                p.getDiseaseResult(),
                p.getPlantedAt(),
                p.getGerminatedAt(),
                p.getMaturedAt(),
                species != null ? species.getId() : null,
                species != null ? species.getMinTemperature() : null,
                species != null ? species.getMaxTemperature() : null,
                species != null ? species.getMinHumidity() : null,
                species != null ? species.getMaxHumidity() : null,
                species != null ? species.getMinPh() : null,
                species != null ? species.getMaxPh() : null,
                species != null ? species.getMinTds() : null,
                species != null ? species.getMaxTds() : null,
                species != null ? species.getStageNames() : null,
                species != null ? species.getStageDurationDays() : null,
                species != null ? species.getStageCount() : null
        );
    }

    private Device findDeviceOwnedByUser(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다: " + serialNumber));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한이 없습니다.");
        }
        return device;
    }
}
