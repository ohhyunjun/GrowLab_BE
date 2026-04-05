package com.metaverse.growlab_be.notice.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.notice.dto.NoticeRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    @Column(nullable = false)
    private String message;

    // 알림 읽음 여부
    @Column(name = "is_read")
    private boolean isRead = false;

    // 알림 유형
    @Column(name = "notice_type")
    private String noticeType;

    // 알림 우선순위
    @Column(nullable = false)
    private Integer priority;

    // 추가 데이터 (예: 센서 값, JSON 등)
    @Column(name = "additional_data")
    private String additionalData;

    // User와의 N:1 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Notice(NoticeRequestDto noticeRequestDto, User user) {
        this.deviceSerial = noticeRequestDto.getDeviceSerial();
        this.message = noticeRequestDto.getMessage();
        this.noticeType = noticeRequestDto.getNoticeType();
        this.priority = noticeRequestDto.getPriority();
        this.additionalData = noticeRequestDto.getAdditionalData();
        this.user = user;
        this.isRead = false;
    }
        public void markAsRead () {
            this.isRead = true;
        }
}