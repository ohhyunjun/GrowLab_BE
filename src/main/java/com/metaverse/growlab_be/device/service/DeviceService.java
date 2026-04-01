package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.DeviceCreateRequestDto;
import com.metaverse.growlab_be.device.dto.DeviceResponseDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final PlantRepository plantRepository;

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
        device.setDeviceNickname(device.getDeviceNickname());

        // 기기의 상태 변경
        device.setStatus(true);
    }


    @Transactional
    public void createDeviceByAdmin(String serialNumber) {
        if (deviceRepository.existsById(serialNumber)) {
            throw new IllegalArgumentException("이미 존재하는 시리얼 번호입니다.");
        }

        Device newDevice = new Device(serialNumber, null);

        deviceRepository.save(newDevice);
    }
}
