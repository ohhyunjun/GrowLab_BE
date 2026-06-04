package com.metaverse.growlab_be.device.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.dto.DeviceResponseDto;
import com.metaverse.growlab_be.device.dto.LedRequestDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorAnomalyRepository;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository        deviceRepository;
    private final PlantRepository         plantRepository;
    private final PhotoRepository         photoRepository;
    private final SensorLogRepository     sensorLogRepository;
    private final SensorAnomalyRepository sensorAnomalyRepository;
    private final NoticeRepository        noticeRepository;
    private final Optional<MqttPublisher> mqttPublisher;

    // ── 기기 등록 ──────────────────────────────────────────────────
    @Transactional
    public void registerDevice(String serialNumber, String deviceNickname, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));

        if (device.getUser() != null) {
            throw new IllegalStateException("이미 등록된 기기입니다.");
        }

        device.setUser(user);
        device.setDeviceNickname(deviceNickname);
        device.setStatus(true);
    }

    // ── 내 기기 목록 조회 ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> getUserDevices(User user) {
        List<Device> devices = deviceRepository.findByUserId(user.getId());

        return devices.stream().map(device -> {
            LocalDateTime lastPhotoAt = photoRepository
                    .findTopByDeviceIdOrderByCreatedAtDesc(device.getId())
                    .map(Photo::getCreatedAt)
                    .orElse(null);

            List<DeviceResponseDto.PlantSummaryDto> plantSummaries = device.getPlants().stream()
                    .map(plant -> new DeviceResponseDto.PlantSummaryDto(
                            plant.getId(),
                            plant.getName(),
                            plant.getPortIndex(),
                            plant.getSpecies().getName(),
                            plant.getPlantStage(),
                            plant.getPlantedAt(),
                            plant.getGerminatedAt(),
                            plant.getMaturedAt()
                    ))
                    .toList();

            return new DeviceResponseDto(device, lastPhotoAt, plantSummaries);
        }).collect(Collectors.toList());
    }

    // ── 어드민 기기 생성 ───────────────────────────────────────────
    @Transactional
    public void createDeviceByAdmin(String serialNumber) {
        if (deviceRepository.existsById(serialNumber)) {
            throw new IllegalArgumentException("이미 존재하는 시리얼 번호입니다.");
        }
        deviceRepository.save(new Device(serialNumber, null));
    }

    // ── 일반 유저 기기 해제 (소유권 해제 + 연관 데이터 정리, serial 유지) ──
    @Transactional
    public void deleteDevice(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다."));

        if (!device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한이 없습니다.");
        }

        if (plantRepository.existsByDeviceId(device.getId())) {
            throw new IllegalStateException("식물이 연결된 기기는 삭제할 수 없습니다.");
        }

        // 이미지 파일 + Photo DB 삭제
        List<Photo> photos = photoRepository.findByDevice(device);
        photos.forEach(photo -> new File(photo.getFilePath()).delete());
        photoRepository.deleteByDevice(device);

        // 연관 데이터 삭제
        sensorLogRepository.deleteByDevice(device);
        sensorAnomalyRepository.deleteByDevice(device);
        noticeRepository.deleteByDeviceSerial(serialNumber);

        // 기기 초기화 (serial 유지)
        device.setUser(null);
        device.setDeviceNickname(null);
        device.setStatus(false);
        device.setLedStatus(false);
        device.setLedMode(false);
        device.setLedOnTime(null);
        device.setLedOffTime(null);
        device.setPhotoInterval(12);
        device.setPortStatus("00000000");
    }

    // ── 어드민 기기 완전 삭제 (serial 포함) ───────────────────────
    @Transactional
    public void deleteDeviceByAdmin(String serialNumber) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기기입니다."));
        deviceRepository.delete(device);
    }

    // ── 촬영 주기 설정 ─────────────────────────────────────────────
    @Transactional
    public void updatePhotoInterval(String serialNumber, Integer hours, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        device.setPhotoInterval(hours);
        mqttPublisher.ifPresent(p -> p.publishPhotoInterval(device.getId(), hours));
    }

    // ── LED 제어 (수동 on/off + 자동 스케줄) ──────────────────────
    @Transactional
    public void controlLed(String serialNumber, LedRequestDto requestDto, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);
        Boolean mode = requestDto.getLedMode();

        if (mode != null) {
            device.setLedMode(mode);
        }

        if (Boolean.FALSE.equals(mode)) {
            // 수동 모드: ledStatus로 on/off
            Boolean ledStatus = requestDto.getLedStatus();
            if (ledStatus != null) {
                device.setLedStatus(ledStatus);
                mqttPublisher.ifPresent(p ->
                        p.publishCommand(device.getId(), ledStatus ? "O" : "o"));
            }
        } else if (Boolean.TRUE.equals(mode)) {
            // 자동 모드: 스케줄 저장 후 RPi 전송
            LocalTime onTime  = requestDto.getLedOnTime();
            LocalTime offTime = requestDto.getLedOffTime();

            if (onTime  != null) device.setLedOnTime(onTime);
            if (offTime != null) device.setLedOffTime(offTime);

            LocalTime finalOn  = device.getLedOnTime();
            LocalTime finalOff = device.getLedOffTime();

            if (finalOn != null && finalOff != null) {
                mqttPublisher.ifPresent(p ->
                        p.publishCommand(device.getId(),
                                "SCHED:" + finalOn + "-" + finalOff));
            }
        }
    }

    // ── 포트 상태 업데이트 ─────────────────────────────────────────
    @Transactional
    public void updatePortStatus(String serialNumber, Integer portIndex,
                                 Boolean status, User user) {
        Device device = findDeviceOwnedByUser(serialNumber, user);

        String current = (device.getPortStatus() != null
                && device.getPortStatus().length() == 8)
                ? device.getPortStatus() : "00000000";

        char[] ports = current.toCharArray();
        ports[portIndex] = status ? '1' : '0';
        device.setPortStatus(new String(ports));

        mqttPublisher.ifPresent(p ->
                p.publishCommand(device.getId(),
                        status ? "P" + portIndex : "p" + portIndex));
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────
    private Device findDeviceOwnedByUser(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기에 대한 권한 없음");
        }
        return device;
    }
}