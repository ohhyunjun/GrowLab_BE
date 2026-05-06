package com.metaverse.growlab_be.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.image.domain.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DiaryResponseDto {
    private Long id;
    private String title;
    private String content;
    private Long plantId;
    private List<String> imageUrls;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime targetDate;

    public DiaryResponseDto(Diary diary) {
        this.id = diary.getId();
        this.plantId = diary.getPlant().getId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.targetDate = diary.getTargetDate();
        this.imageUrls = diary.getImages().stream().map(Image::getImgUrl).toList();
    }
}
