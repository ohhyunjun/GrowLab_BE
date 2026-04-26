package com.metaverse.growlab_be.image.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.image.domain.Image;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ImageResponseDto {
    private Long id;
    private String imgUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    public ImageResponseDto(Image image) {
        this.id = image.getId();
        this.imgUrl = image.getImgUrl();
        this.createdAt = image.getCreatedAt();
    }
}