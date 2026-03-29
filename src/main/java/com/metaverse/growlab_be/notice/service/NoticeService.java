package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.notice.domain.Notice;
import com.metaverse.growlab_be.notice.dto.NoticeResponseDto;
import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 모든 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getAllNotices(User user) {
        return noticeRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(NoticeResponseDto::new)
                .toList();
    }

    // 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getUnreadNotices(User user) {
        return noticeRepository.findAllByUserAndIsReadFalseOrderByCreatedAtDesc(user).stream()
                .map(NoticeResponseDto::new)
                .toList();
    }

    // 읽지 않은 알림 개수 조회
    @Transactional(readOnly = true)
    public Long getUnreadCount(User user) {
        return noticeRepository.countByUserAndIsReadFalse(user);
    }

    // 특정 알림 읽음 처리
    @Transactional
    public void readNotice(Long noticeId, User user) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));

        // 보안: 알림의 주인이 로그인한 유저인지 확인
        validateOwner(notice, user);

        notice.markAsRead(); // 엔티티의 isRead 필드를 true로 변경
    }

    // 모든 알림 읽음 처리
    @Transactional
    public void readAllNotices(User user) {
        List<Notice> unreadNotices = noticeRepository.findAllByUserAndIsReadFalse(user);
        unreadNotices.forEach(Notice::markAsRead);
    }

    // 특정 알림 삭제
    @Transactional
    public void deleteNotice(Long noticeId, User user) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));

        // 보안 검증
        validateOwner(notice, user);

        noticeRepository.delete(notice);
    }

    // [공통 로직]: 본인 확인 검증
    private void validateOwner(Notice notice, User user) {
        if (!notice.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 알림에 대한 접근 권한이 없습니다.");
        }
    }
}
