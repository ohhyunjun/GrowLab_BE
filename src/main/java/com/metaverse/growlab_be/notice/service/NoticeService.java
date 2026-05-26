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

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    // 모든 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getAllNotices(Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        return noticeRepository.findAllByUserOrderByCreatedAtDesc(foundUser).stream()
                .map(NoticeResponseDto::new)
                .toList();
    }

    // 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getUnreadNotices(Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        return noticeRepository.findAllByUserAndIsReadFalseOrderByCreatedAtDesc(foundUser).stream()
                .map(NoticeResponseDto::new)
                .toList();
    }

    // 읽지 않은 알림 개수 조회
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        return noticeRepository.countByUserAndIsReadFalse(foundUser);
    }

    // 특정 알림 읽음 처리
    @Transactional
    public void readNotice(Long noticeId, Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        // 알림 조회: noticeId와 로그인한 유저를 기준으로 알림 조회
        Notice notice = noticeRepository.findByIdAndUser(noticeId, foundUser)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않거나 접근 권한이 없습니다."));

        notice.markAsRead(); // 엔티티의 isRead 필드를 true로 변경
    }

    // 모든 알림 읽음 처리
    @Transactional
    public void readAllNotices(Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        List<Notice> unreadNotices = noticeRepository.findAllByUserAndIsReadFalse(foundUser);
        unreadNotices.forEach(Notice::markAsRead);
    }

    // 특정 알림 삭제
    @Transactional
    public void deleteNotice(Long noticeId, Long userId) {
        // 로그인한 유저의 ID로 유저 정보 조회
        User foundUser = getValidUserById(userId);

        Notice notice = noticeRepository.findByIdAndUser(noticeId, foundUser)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않거나 삭제 권한이 없습니다."));

        noticeRepository.delete(notice);
    }

    @Transactional
    public void createAnalysisNotice(Device device, String message, NoticeType type, Integer priority){
        if (device.getUser() == null) return;;
        Notice notice = new Notice(
                device.getId(), message, type, priority, null, device.getUser());
        noticeRepository.save(notice);
    }

    @Transactional
    public void createAlertFromRpi(NoticeRequestDto dto) {
        // 시리얼 번호로 기기 찾기
        Device device = deviceRepository.findById(dto.getSerial_number())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시리얼 번호입니다."));

        // 기기에 유저가 없으면 무시
        if (device.getUser() == null) return;

        String sensor = dto.getSensor().trim().toUpperCase();

        // 미읽음 동일 센서 notice는 생성 안함
        boolean unreadExists = noticeRepository
                .existsByUserAndDeviceSerialAndAdditionalDataContainingAndIsReadFalse(
                        device.getUser(), dto.getSerial_number(), sensor);
        if (unreadExists) return;

        // 쿨다운 체크
        int cooldownHours = COOLDOWN_HOURS.getOrDefault(sensor, 12);
        LocalDateTime cooldownTime = LocalDateTime.now().minusHours(cooldownHours);
        boolean recentExists = noticeRepository
                .existsByUserAndDeviceSerialAndAdditionalDataContainingAndCreatedAtAfter(
                        device.getUser(), dto.getSerial_number(), sensor, cooldownTime);
        if (recentExists) return;

        // 메시지 생성
        String message;
        String additionalData;
        if ("WATER".equals(sensor)) {
            message = "[물 부족] 수위가 낮습니다. 물을 보충해주세요.";
            additionalData = "{\"sensor\":\"WATER\",\"value\":0}";
        } else {
            message = String.format("[%s] 이상값 감지: %.2f", dto.getSensor(), dto.getValue());
            additionalData = String.format(
                    "{\"sensor\":\"%s\",\"value\":%.2f}", dto.getSensor(), dto.getValue()
            );
        }

        Notice notice = new Notice(
                dto.getSerial_number(),
                message,
                NoticeType.SENSOR_ALERT,
                1,
                additionalData,
                device.getUser()
        );
        noticeRepository.save(notice);
    }

    // [공통 검증 메서드]
    //  본인 확인 검증
    private void validateOwner(Notice notice, User user) {
        if (!notice.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 알림에 대한 접근 권한이 없습니다.");
        }
    }

    // 알림 존재 여부 확인
    private Notice getValidNoticeById(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(() ->
                new IllegalArgumentException("해당 알림이 존재하지 않습니다. (ID: " + noticeId + ")"));
    }

    // 유저 존재 여부 검증 및 객체 반환
    private User getValidUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 사용자입니다. (ID: " + userId + ")"));

    }

    // 센서별 쿨다운 시간
    private static final Map<String, Integer> COOLDOWN_HOURS = Map.of(
            "TDS", 12,
            "PH", 12,
            "TEMP", 4,
            "HUM", 4,
            "WATER", 4
    );


}
