package com.metaverse.growlab_be.diary.dto;

import com.metaverse.growlab_be.diary.domain.Diary;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DiaryResponseDto {
    private Long id;
    private String title;
    private String content;
    private String targetDate;

    public DiaryResponseDto(Diary diary) {
        this.id = diary.getId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.targetDate = diary.getTargetDate().toString();
    }
}
