package com.metaverse.growlab_be.notice.repository;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.notice.domain.Notice;
import com.metaverse.growlab_be.notice.domain.NoticeType; // [필수] 반드시 추가
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 유저의 모든 알림을 최신순으로 조회
    List<Notice> findAllByUserOrderByCreatedAtDesc(User user);

    // 유저의 '읽지 않은' 알림만 최신순으로 조회
    List<Notice> findAllByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // 읽지 않은 알림 조회 (최신순)
    List<Notice> findAllByUserAndIsReadFalse(User user);

    // 읽지 않은 알림 개수 조회
    long countByUserAndIsReadFalse(User user);

    // 특정 알림을 유저와 함께 조회
    Optional<Notice> findByIdAndUser(Long id, User user);

    // 미읽음 동일 센서 존재 여부
    boolean existsByUserAndDeviceSerialAndAdditionalDataContainingAndIsReadFalse(
            User user, String deviceSerial, String sensor);

    // 쿨다운 내 동일 센서 존재 여부
    boolean existsByUserAndDeviceSerialAndAdditionalDataContainingAndCreatedAtAfter(
            User user, String deviceSerial, String sensor, LocalDateTime time);

    // ─────────────────────────────────────────────────────────────
    // 카메라 분석용: 미읽음 특정 알림 타입(예: SENSOR_ALERT) 존재 여부
    boolean existsByUserAndDeviceSerialAndNoticeTypeAndIsReadFalse(
            User user, String deviceSerial, NoticeType noticeType);

    // 카메라 분석용: 쿨다운 내 특정 알림 타입(예: SENSOR_ALERT) 존재 여부
    boolean existsByUserAndDeviceSerialAndNoticeTypeAndCreatedAtAfter(
            User user, String deviceSerial, NoticeType noticeType, LocalDateTime time);

    // NoticeRepository.java에 추가
    void deleteByDeviceSerial(String deviceSerial);
}
