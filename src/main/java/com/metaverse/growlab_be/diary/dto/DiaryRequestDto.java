package com.metaverse.growlab_be.diary.dto;

import lombok.Getter;

@Getter
public class DiaryRequestDto {
    private String title;
    private String content;
    private String targetDate;
}
