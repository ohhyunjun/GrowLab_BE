package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.notice.domain.Notice;
import com.metaverse.growlab_be.notice.dto.NoticeResponseDto;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

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
}
