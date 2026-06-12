package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.*;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final PhotoRepository  photoRepository;
    private final SpeciesRepository speciesRepository;

    public List<DeviceResponseDto> getUserDevices(User user) {
        List<Device> devices = deviceRepository.findByUserId(user.getId());
        return devices.stream()
                .map(device -> {
                    LocalDateTime lastPhotoAt = photoRepository
                            .findTopByDeviceIdOrderByCreatedAtDesc(device.getId())
                            .map(Photo::getCreatedAt)
                            .orElse(null);

                    List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getPlants().stream()
                            .map(p -> new DeviceResponseDto.PlantSummaryDto(
                                    p.getId(),
                                    p.getName(),
                                    p.getPortIndex(),
                                    p.getSpecies() != null ? p.getSpecies().getName() : null,
                                    p.getPlantStage(),
                                    p.getPlantedAt(),
                                    p.getGerminatedAt(),
                                    p.getMaturedAt()
                            ))
                            .sorted(Comparator.comparingInt(DeviceResponseDto.PlantSummaryDto::getPortIndex))
                            .toList();

                    return new DeviceResponseDto(device, lastPhotoAt, plantSummaries);
                })
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
        device.setUser(null);
        device.setSpecies(null);
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
    }

    @Transactional
    public void controlLed(String serialNumber, LedRequestDto requestDto, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        if (Boolean.TRUE.equals(requestDto.getLedMode())) {
            device.setLedMode(true);
            device.setLedOnTime(requestDto.getLedOnTime());
            device.setLedOffTime(requestDto.getLedOffTime());
        } else {
            device.setLedMode(false);
            device.setLedStatus(requestDto.getLedStatus());
        }
    }

    @Transactional
    public void updatePortStatus(String serialNumber, Integer portIndex, Boolean status, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        char[] bits = device.getPortStatus().toCharArray();
        bits[portIndex] = status ? '1' : '0';
        device.setPortStatus(new String(bits));
    }

    // ✅ 기기 대표 품종 설정/변경
    @Transactional
    public DeviceResponseDto updateDeviceSpecies(String serialNumber, Long speciesId, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);

        // 포트가 하나라도 켜져 있으면 품종 변경 불가
        String portStatus = device.getPortStatus();
        if (portStatus != null && portStatus.contains("1")) {
            throw new IllegalStateException("포트가 활성화된 상태에서는 품종을 변경할 수 없습니다.");
        }

        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품종입니다: " + speciesId));

        device.setSpecies(species);

        List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getPlants().stream()
                .map(p -> new DeviceResponseDto.PlantSummaryDto(
                        p.getId(), p.getName(), p.getPortIndex(),
                        p.getSpecies() != null ? p.getSpecies().getName() : null,
                        p.getPlantStage(), p.getPlantedAt(), p.getGerminatedAt(), p.getMaturedAt()
                ))
                .sorted(Comparator.comparingInt(DeviceResponseDto.PlantSummaryDto::getPortIndex))
                .toList();

        return new DeviceResponseDto(device, null, plantSummaries);
    }

    // 헬퍼
    private Device findDeviceOwnedByUser(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다: " + serialNumber));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한이 없습니다.");
        }
        return device;
    }
}