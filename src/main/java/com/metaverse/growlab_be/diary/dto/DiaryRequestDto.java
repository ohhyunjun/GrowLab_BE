package com.metaverse.growlab_be.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class DiaryRequestDto {
    private String title;
    private String content;

    // JSON 데이터가 이 형식으로 들어오면 자동으로 LocalDateTime으로 변환됩니다.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime targetDate;
}
