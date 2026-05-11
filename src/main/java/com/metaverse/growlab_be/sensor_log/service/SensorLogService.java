package com.metaverse.growlab_be.sensor_log.service;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogResponseDto;
import com.metaverse.growlab_be.sensor_log.repository.SensorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class SensorLogService {
    private final SensorLogRepository sensorLogRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public SensorLogResponseDto createSensorLog (SensorLogRequestDto sensorLogRequestDto) {
        // 시리얼 번호로 DB에서 기기 찾기
        Device device = getValidDeviceById(sensorLogRequestDto.getSerial_number());

        // sensorlog 엔터티 생성
        SensorLog sensorLog = new SensorLog(
                device,
                sensorLogRequestDto.getTemperature(),
                sensorLogRequestDto.getHumidity(),
                sensorLogRequestDto.getPh(),
                sensorLogRequestDto.getTds(),
                sensorLogRequestDto.getWater_level_status()
        );
        SensorLog savedLog = sensorLogRepository.save(sensorLog);
        return new SensorLogResponseDto(savedLog);

    }

    @Transactional
    public SensorLogResponseDto getLatestSensorLog (String serialNumber, PrincipalDetails  principalDetails) {
        Device device = getValidDeviceById(serialNumber);

        if (device.getUser() == null || !device.getUser().getId().equals(principalDetails.user().getId())){
            throw new IllegalArgumentException("해당 기기에 대한 접근 권한이 없습니다.");
        }

        SensorLog latestLog = sensorLogRepository.findTopBySerialNumberOrderByCreatedAtDesc(device)
                .orElseThrow(()-> new IllegalArgumentException("측정된 센서 데이터가 없습니다."));

        return new SensorLogResponseDto(latestLog);
    }

    public Device getValidDeviceById(String serialNumber){
        return deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));
    }

}
