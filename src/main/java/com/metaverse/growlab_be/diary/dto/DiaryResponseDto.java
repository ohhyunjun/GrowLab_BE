package com.metaverse.growlab_be.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.image.domain.Image;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DiaryResponseDto {
    private final Long id;
    private final String title;
    private final String content;
    private final String deviceSerial;
    private final String deviceNickname;
    private final Integer portIndex;
    private final String speciesName;
    private final String plantName;
    private final List<String> imageUrls;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime targetDate;

    public DiaryResponseDto(Diary diary) {
        this.id = diary.getId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.deviceSerial = diary.getDevice().getId();
        this.deviceNickname = diary.getDevice().getDeviceNickname();
        this.portIndex = diary.getPortIndex();
        this.speciesName = diary.getSpeciesName();
        this.plantName = diary.getPlantName();
        this.targetDate = diary.getTargetDate();
        this.imageUrls = diary.getImages().stream().map(Image::getImgUrl).toList();
    }
}
