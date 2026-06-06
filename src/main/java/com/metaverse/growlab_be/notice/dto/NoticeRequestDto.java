package com.metaverse.growlab_be.notice.dto;

import lombok.Getter;

@Getter
public class NoticeRequestDto {
    private String serial_number;
    private String sensor;
    private Float value;
}
