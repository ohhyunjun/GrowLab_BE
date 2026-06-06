package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
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
    public void createAnalysisNotice(Device device, String message, NoticeType type, Integer priority) {
        User user = device.getUser();
        if (user == null) return;

        if (type == NoticeType.SENSOR_ALERT) {
            boolean hasUnread = noticeRepository.existsByUserAndDeviceSerialAndNoticeTypeAndIsReadFalse(
                    user, device.getId(), type);
            if (hasUnread) return;

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            boolean hasRecent = noticeRepository.existsByUserAndDeviceSerialAndNoticeTypeAndCreatedAtAfter(
                    user, device.getId(), type, oneHourAgo);
            if (hasRecent) return;
        }

        Notice notice = new Notice(device.getId(), message, type, priority, "{\"source\":\"camera_analysis\"}", user);
        noticeRepository.save(notice);
    }

    // [센서 기반] RPi 알림 처리 (WATER 메시지 분기 복구)
    @Transactional
    public void createAlertFromRpi(NoticeRequestDto dto) {
        Device device = deviceRepository.findById(dto.getSerial_number())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다."));

        User user = device.getUser();
        if (user == null) return;

        boolean hasUnread = noticeRepository.existsByUserAndDeviceSerialAndAdditionalDataContainingAndIsReadFalse(
                user, device.getId(), dto.getSensor());
        if (hasUnread) return;

        int cooldownHours = COOLDOWN_HOURS.getOrDefault(dto.getSensor().toUpperCase(), 1);
        LocalDateTime cooldownTime = LocalDateTime.now().minusHours(cooldownHours);

        boolean hasRecent = noticeRepository.existsByUserAndDeviceSerialAndAdditionalDataContainingAndCreatedAtAfter(
                user, device.getId(), dto.getSensor(), cooldownTime);
        if (hasRecent) return;

        // [수정] WATER 센서 특별 메시지 분기 처리 복구
        String message;
        String additionalData;
        if ("WATER".equalsIgnoreCase(dto.getSensor())) {
            message = "[물 부족] 수위가 낮습니다.";
            additionalData = "{\"sensor\":\"WATER\", \"value\":0}";
        } else {
            message = String.format("[%s] 이상값 감지: %.2f", dto.getSensor(), dto.getValue());
            additionalData = String.format("{\"sensor\":\"%s\",\"value\":%.2f}", dto.getSensor(), dto.getValue());
        }

        Notice notice = new Notice(dto.getSerial_number(), message, NoticeType.SENSOR_ALERT, 1, additionalData, user);
        noticeRepository.save(notice);
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
