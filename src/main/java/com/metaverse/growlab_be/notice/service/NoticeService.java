package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.service.SharedEnvironment;
import com.metaverse.growlab_be.notice.domain.Notice;
import com.metaverse.growlab_be.notice.domain.NoticeType;
import com.metaverse.growlab_be.notice.dto.NoticeRequestDto;
import com.metaverse.growlab_be.notice.dto.NoticeResponseDto;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    private static final Map<String, Integer> COOLDOWN_HOURS = Map.of(
            "TDS", 12,
            "PH", 12,
            "TEMP", 4,
            "HUM", 4,
            "WATER", 4
    );

    // 모든 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getAllNotices(Long userId) {
        User foundUser = getValidUserById(userId);
        return noticeRepository.findAllByUserOrderByCreatedAtDesc(foundUser).stream()
                .map(NoticeResponseDto::new)
                .collect(Collectors.toList());
    }

    // 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getUnreadNotices(Long userId) {
        User foundUser = getValidUserById(userId);
        return noticeRepository.findAllByUserAndIsReadFalseOrderByCreatedAtDesc(foundUser).stream()
                .map(NoticeResponseDto::new)
                .collect(Collectors.toList());
    }

    // [수정] 읽지 않은 알림 개수 조회 메서드 추가
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        User foundUser = getValidUserById(userId);
        return noticeRepository.countByUserAndIsReadFalse(foundUser);
    }

    // 모든 알림 읽음 처리
    @Transactional
    public void readAllNotices(Long userId) {
        User foundUser = getValidUserById(userId);
        List<Notice> unreadNotices = noticeRepository.findAllByUserAndIsReadFalse(foundUser);
        for (Notice notice : unreadNotices) {
            notice.setRead(true);
        }
    }

    // 단일 알림 읽음 처리
    @Transactional
    public void readNotice(Long noticeId, Long userId) {
        User user = getValidUserById(userId);
        Notice notice = getValidNoticeById(noticeId);
        validateOwner(notice, user);
        notice.setRead(true);
    }

    // 단일 알림 삭제
    @Transactional
    public void deleteNotice(Long noticeId, Long userId) {
        User user = getValidUserById(userId);
        Notice notice = getValidNoticeById(noticeId);
        validateOwner(notice, user);
        noticeRepository.delete(notice);
    }

    // [카메라 기반] 분석 알림 생성
    @Transactional
    public void createAnalysisNotice(Device device, Plant plant, String message, NoticeType type, Integer priority) {
        User user = device.getUser();
        if (user == null || plant.getId() == null) return;

        String marker = "\"source\":\"camera_analysis\",\"plantId\":" + plant.getId();
        if (type == NoticeType.SENSOR_ALERT) {
            // 질병 알림은 이 식물의 카메라 알림만 중복 검사한다.
            boolean hasUnread = noticeRepository.existsByUserAndDeviceSerialAndNoticeTypeAndAdditionalDataContainingAndIsReadFalse(
                    user, device.getId(), NoticeType.SENSOR_ALERT, marker);
            if (hasUnread) return;

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            boolean hasRecent = noticeRepository.existsByUserAndDeviceSerialAndNoticeTypeAndAdditionalDataContainingAndCreatedAtAfter(
                    user, device.getId(), NoticeType.SENSOR_ALERT, marker, oneHourAgo);
            if (hasRecent) return;
        }

        String additionalData = "{" + marker + "}";
        noticeRepository.save(new Notice(device.getId(), message, type, priority, additionalData, user));
    }

    private static final Set<String> ENVIRONMENT_SENSORS = Set.of("TEMP", "HUM", "PH", "TDS");

    // Arduino detects an out-of-range reading; RPi forwards its alert here.
    @Transactional
    public void createAlertFromRpi(NoticeRequestDto dto) {
        if (dto.getSensor() == null || dto.getSensor().isBlank()) {
            throw new IllegalArgumentException("센서 종류가 필요합니다.");
        }
        String sensor = dto.getSensor().trim().toUpperCase(Locale.ROOT);
        Float value = dto.getValue();
        if (!"WATER".equals(sensor) && (value == null || !Float.isFinite(value))) {
            throw new IllegalArgumentException("유효한 센서값이 필요합니다.");
        }

        Device device = deviceRepository.findById(dto.getSerial_number())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다."));
        User user = device.getUser();
        if (user == null) return;

        if (ENVIRONMENT_SENSORS.contains(sensor)) {
            if (device.getPlants().isEmpty()) return;
            try {
                SharedEnvironment current = SharedEnvironment.fromSpecies(
                        device.getPlants().stream().map(Plant::getSpecies).toList());
                if (!rangeFor(current, sensor).outside(value)) return;
            } catch (IllegalArgumentException | IllegalStateException invalidRanges) {
                return;
            }
        }

        String marker = "\"sensor\":\"" + sensor + "\"";
        if (noticeRepository.existsByUserAndDeviceSerialAndAdditionalDataContainingAndIsReadFalse(
                user, device.getId(), marker)) return;
        int cooldownHours = COOLDOWN_HOURS.getOrDefault(sensor, 1);
        if (noticeRepository.existsByUserAndDeviceSerialAndAdditionalDataContainingAndCreatedAtAfter(
                user, device.getId(), marker, LocalDateTime.now().minusHours(cooldownHours))) return;

        String message;
        String additionalData;
        if ("WATER".equals(sensor)) {
            message = "[물 부족] 수위가 낮습니다.";
            additionalData = "{\"source\":\"arduino_alert\",\"sensor\":\"WATER\",\"value\":0}";
        } else {
            String label = device.getDeviceNickname() == null ? device.getId() : device.getDeviceNickname();
            String affected = affectedSpecies(device, sensor, value);
            String suffix = affected.isBlank() ? "" : " (영향 품종: " + affected + ")";
            message = String.format(Locale.ROOT, "[%s] %s 적정 범위 이탈: %.2f%s",
                    label, sensor, value, suffix);
            additionalData = String.format(Locale.ROOT,
                    "{\"source\":\"arduino_alert\",\"sensor\":\"%s\",\"value\":%.2f}",
                    sensor, value);
        }
        noticeRepository.save(new Notice(device.getId(), message, NoticeType.SENSOR_ALERT, 1,
                additionalData, user));
    }

    private String affectedSpecies(Device device, String sensor, Float value) {
        if (!ENVIRONMENT_SENSORS.contains(sensor)) return "";
        return device.getPlants().stream().map(Plant::getSpecies)
                .filter(species -> rangeFor(species, sensor).outside(value))
                .map(Species::getName).distinct().collect(Collectors.joining(", "));
    }

    private SharedEnvironment.Range rangeFor(SharedEnvironment environment, String sensor) {
        return switch (sensor) {
            case "TEMP" -> environment.temperature();
            case "HUM" -> environment.humidity();
            case "PH" -> environment.ph();
            case "TDS" -> environment.tds();
            default -> throw new IllegalArgumentException("지원하지 않는 센서입니다.");
        };
    }

    private SharedEnvironment.Range rangeFor(Species species, String sensor) {
        return switch (sensor) {
            case "TEMP" -> new SharedEnvironment.Range(species.getMinTemperature(), species.getMaxTemperature());
            case "HUM" -> new SharedEnvironment.Range(species.getMinHumidity(), species.getMaxHumidity());
            case "PH" -> new SharedEnvironment.Range(species.getMinPh(), species.getMaxPh());
            case "TDS" -> new SharedEnvironment.Range(species.getMinTds(), species.getMaxTds());
            default -> throw new IllegalArgumentException("지원하지 않는 센서입니다.");
        };
    }

    private void validateOwner(Notice notice, User user) {
        if (!notice.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 알림에 대한 접근 권한이 없습니다.");
        }
    }

    private Notice getValidNoticeById(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(() ->
                new IllegalArgumentException("해당 알림이 존재하지 않습니다. (ID: " + noticeId + ")"));
    }

    private User getValidUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 사용자입니다. (ID: " + userId + ")"));
    }
}
