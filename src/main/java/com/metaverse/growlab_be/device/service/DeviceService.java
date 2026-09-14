package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.*;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
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
    private final SpeciesRepository speciesRepository;
    private final ObjectProvider<MqttPublisher> mqttPublisherProvider;

    // 배포 전에 이미 품종이 지정돼 있던 기기도 retained thresholds를 받도록 재발행한다.
    @EventListener(ApplicationReadyEvent.class)
    public void publishRetainedThresholdsOnStartup() {
        mqttPublisherProvider.ifAvailable(publisher ->
                deviceRepository.findAll().forEach(device -> {
                    try {
                        publisher.publishThresholds(device.getId(), device.getSpecies());
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

                    List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getPlants().stream()
                            .map(p -> buildPlantSummary(device, p))
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
        device.getPlants().clear();
        device.setUser(null);
        device.setSpecies(null);
        device.setPortStatus("00000000");

        // retained 메시지에 이전 품종 범위가 남지 않도록 제한 없음 값으로 초기화한다.
        mqttPublisherProvider.ifAvailable(publisher ->
                publisher.publishThresholds(serialNumber, null));
    }

    @Transactional
    public void deleteDeviceByAdmin(String serialNumber) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다: " + serialNumber));
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
        Device device = findDeviceOwnedByUser(serialNumber, user);
        char[] bits = device.getPortStatus().toCharArray();
        bits[portIndex] = status ? '1' : '0';
        device.setPortStatus(new String(bits));
    }

    @Transactional
    public DeviceResponseDto updateDeviceSpecies(String serialNumber, Long speciesId, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);

        String portStatus = device.getPortStatus();
        if (portStatus != null && portStatus.contains("1")) {
            throw new IllegalStateException("포트가 활성화된 상태에서는 품종을 변경할 수 없습니다.");
        }

        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품종입니다: " + speciesId));

        device.setSpecies(species);

        mqttPublisherProvider.ifAvailable(publisher ->
                publisher.publishThresholds(serialNumber, species));

        List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getPlants().stream()
                .map(p -> buildPlantSummary(device, p))
                .sorted(Comparator.comparingInt(DeviceResponseDto.PlantSummaryDto::getPortIndex))
                .toList();

        return new DeviceResponseDto(device, null, plantSummaries);
    }

    // ✅ 공통: Plant + 소속 기기의 대표 품종을 조합해 요약 DTO 생성
    private DeviceResponseDto.PlantSummaryDto buildPlantSummary(Device device, com.metaverse.growlab_be.plant.domain.Plant p) {
        Species species = device.getSpecies();
        return new DeviceResponseDto.PlantSummaryDto(
                p.getId(),
                p.getName(),
                p.getPortIndex(),
                species != null ? species.getName() : null,
                p.getStageIndex(),
                species != null ? species.getStageName(p.getStageIndex()) : null,
                p.getPlantedAt(),
                p.getGerminatedAt(),
                p.getMaturedAt()
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
