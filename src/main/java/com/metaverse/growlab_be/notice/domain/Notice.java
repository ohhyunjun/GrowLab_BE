package com.metaverse.growlab_be.notice.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notice")
public class Notice extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 디바이스의 시리얼 번호
    @Column(name = "device_serial")
    private String deviceSerial;

    // 알림 메시지
    @Column(length = 500, nullable = false, columnDefinition = "TEXT")
    private String message;

    // 알림 읽음 여부
    @Column(name = "is_read")
    private boolean isRead = false;

    // 알림 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NoticeType noticeType;

    // 알림 우선순위 (1: 높음, 2: 보통, 3: 낮음)
    @Column(nullable = false)
    private Integer priority = 2;

    // 추가 데이터 (예: 센서 값, JSON 등)
    @Column(length = 1000)
    private String additionalData;

    // User와의 N:1 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Notice(String deviceSerial, String message, NoticeType noticeType, Integer priority, String additionalData, User user) {
        this.deviceSerial = deviceSerial;
        this.message = message;
        this.noticeType = noticeType;
        this.priority = priority != null ? priority : 2; // 기본값 처리
        this.additionalData = additionalData;
        this.user = user;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}