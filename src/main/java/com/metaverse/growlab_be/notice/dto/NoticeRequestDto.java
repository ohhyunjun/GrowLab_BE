package com.metaverse.growlab_be.notice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeRequestDto {
    private String deviceSerial;
    private String message;
    private String noticeType;
    private Integer priority;
    private String additionalData;
}
