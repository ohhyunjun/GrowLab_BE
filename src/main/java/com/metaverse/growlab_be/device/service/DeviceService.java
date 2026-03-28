package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;

    public void registerDevice(String serialNumber, User user) {
        // 시리얼 번호로 DB에서 기기 찾기
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
        // 기기에 다른 유저가 등록되어 있는지 확인
        if (device.getUser() != null) {
            throw new IllegalStateException("이미 등록된 기기입니다.");
        }
        // 기기의 유저 정보를 현재 로그인한 유저로 설정
        device.setUser(user);
        // 기기의 상태 변경
        device.setStatus(true);
    }
}
