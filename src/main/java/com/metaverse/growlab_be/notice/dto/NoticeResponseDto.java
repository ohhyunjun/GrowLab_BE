package com.metaverse.growlab_be.notice.dto;

import com.metaverse.growlab_be.notice.domain.Notice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class NoticeResponseDto {
    private Long id;
    private String deviceSerial;
    private String message;
    private boolean isRead;
    private String noticeType;
    private Integer priority;
    private String additionalData;

    public NoticeResponseDto(Notice notice) {
        this.id = notice.getId();
        this.deviceSerial = notice.getDeviceSerial();
        this.message = notice.getMessage();
        this.isRead = notice.isRead();
        this.noticeType = notice.getNoticeType();
        this.priority = notice.getPriority();
        this.additionalData = notice.getAdditionalData();
    }
}
