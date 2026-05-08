package com.metaverse.growlab_be.photo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // @ModelAttribute는 필드에 값을 할당하기 위해 Setter가 필요합니다.
public class PhotoRequestDto {
    private String bestResult;
    private Double avgConfidence;
    private Integer totalDetected;
    private String classSummary;
    private String detections;
    private MultipartFile imageFile;
    private String serialNumber;

}