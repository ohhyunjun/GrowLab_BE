package com.metaverse.growlab_be.photo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // @ModelAttribute는 필드에 값을 할당하기 위해 Setter가 필요합니다.
public class PhotoRequestDto {
    private MultipartFile imageFile;
    private String serialNumber;
    private Integer portIndex;

    private String growthResult;      // "sprout", "growth", "no_detection"
    private Double growthConfidence;

    private String diseaseResult;     // "healthy", "disease", 질병명, "no_detection"
    private Double diseaseConfidence;

}