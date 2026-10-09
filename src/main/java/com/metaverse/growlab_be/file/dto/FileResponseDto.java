package com.metaverse.growlab_be.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.file.domain.File;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FileResponseDto {
    private final Long id;
    private final String imgUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private final LocalDateTime createdAt;

    public FileResponseDto(File file) {
        this.id = file.getId();
        this.imgUrl = file.getFileUrl();
        this.createdAt = file.getCreatedAt();
    }
}
