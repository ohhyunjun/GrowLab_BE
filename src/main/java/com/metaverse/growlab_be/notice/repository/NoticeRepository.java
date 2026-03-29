package com.metaverse.growlab_be.notice.repository;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 유저의 모든 알림을 최신순으로 조회
    List<Notice> findAllByUserOrderByCreatedAtDesc(User user);

    // 유저의 '읽지 않은' 알림만 최신순으로 조회
    List<Notice> findAllByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // 읽지 않은 알림 조회 (최신순)
    List<Notice> findAllByUserAndIsReadFalse(User user);

    // 읽지 않은 알림 개수 조회
    long countByUserAndIsReadFalse(User user);
}
