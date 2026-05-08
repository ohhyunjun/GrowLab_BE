package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.DeviceCreateRequestDto;
import com.metaverse.growlab_be.device.dto.DeviceResponseDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final PlantRepository plantRepository;
    private final PhotoRepository photoRepository;
    // 서버에서 라즈베리파이 통신 방법(p, O, o를 보낼 예정)
    private final Optional<MqttPublisher> mqttPublisher;

    @Transactional
    public void registerDevice(String serialNumber, String deviceNickname ,User user) {
        // 시리얼 번호로 DB에서 기기 찾기
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
        // 기기에 다른 유저가 등록되어 있는지 확인
        if (device.getUser() != null) {
            throw new IllegalStateException("이미 등록된 기기입니다.");
        }
        // 기기의 유저 정보를 현재 로그인한 유저로 설정
        device.setUser(user);

        // 기기 닉네임을 설정하는 로직
        device.setDeviceNickname(deviceNickname);

        // 기기의 상태 변경
        device.setStatus(true);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponseDto> getUserDevices(User user) {
        List<Device> devices = deviceRepository.findByUserId(user.getId());

        return devices.stream().map(device -> {
            //Photo 테이블에서 마지막 촬영 시각 조회
            LocalDateTime lastPhotoAt = photoRepository
                    .findTopByDeviceIdOrderByCreatedAtDesc(device.getId())
                    .map(photo -> photo.getCreatedAt())
                    .orElse(null);

            DeviceResponseDto.PlantSummaryDto plantSummary = plantRepository.findByDeviceId(device.getId())
                    .map(plant -> new DeviceResponseDto.PlantSummaryDto(
                            plant.getId(),
                            plant.getName(),
                            plant.getSpecies().getName(),
                            plant.getPlantStage()
                    ))
                    .orElse(null);

            return new DeviceResponseDto(device, lastPhotoAt, plantSummary);
        }).collect(Collectors.toList());
    }


    @Transactional
    public void createDeviceByAdmin(String serialNumber) {
        if (deviceRepository.existsById(serialNumber)) {
            throw new IllegalArgumentException("이미 존재하는 시리얼 번호입니다.");
        }

        Device newDevice = new Device(serialNumber, null);

        deviceRepository.save(newDevice);
    }

    @Transactional
    public void deleteDevice(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다."));

        if (!device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한이 없습니다.");
        }

        boolean hasPlant = plantRepository.findByDeviceId(device.getId()).isPresent();
        if (hasPlant) {
            throw new IllegalStateException("식물이 연결된 기기는 삭제할 수 없습니다.");
        }

        deviceRepository.delete(device);
    }

    // 사용자 웹에서 촬영주기 설정시 이를 db 저장 및 mqtt로 라즈베리파이에게 전송
    @Transactional
    public void updatePhotoInterval(String serialNumber, Integer hours, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        device.setPhotoInterval(hours);
        mqttPublisher.ifPresent(publisher ->
                publisher.publishPhotoInterval(device.getId(), hours));
    }

    @Transactional
    public void controlLed(String serialNumber, boolean on, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        device.setLedStatus(on);
        mqttPublisher.ifPresent(publisher ->
                publisher.publishCommand(device.getId(), on ? "O" : "o"));
    }
    private Device findDeviceOwnedByUser(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한 없음");
        }
        return device;
    }

}
