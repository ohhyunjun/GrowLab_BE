package com.metaverse.growlab_be.notice.dto;

import com.metaverse.growlab_be.notice.domain.Notice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public NoticeResponseDto(Notice notice) {
        this.id = notice.getId();
        this.deviceSerial = notice.getDeviceSerial();
        this.message = notice.getMessage();
        this.isRead = notice.isRead();
        this.noticeType = notice.getNoticeType().name();
        this.priority = notice.getPriority();
        this.additionalData = notice.getAdditionalData();
        this.createdAt = notice.getCreatedAt();
    }
}
