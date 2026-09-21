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

    private String diseaseResult;     // 정상: "healthy" 또는 "Normal", 그 외 질병명, 미검출: "no_detection"
    private Double diseaseConfidence;

    // 전체 식물 박스 크기(최대 면적 박스 기준). RPi 구버전은 안 보내므로 모두 nullable.
    private Double boxWidth;
    private Double boxHeight;
    private Double boxArea;
    private Double boxAreaRatio;

    // 검출된 전체 박스 원본(정규화 좌표 JSON 배열)과 대표로 고른 박스 인덱스. 역시 nullable.
    private String detectionsJson;
    private Integer selectedBoxIndex;

}