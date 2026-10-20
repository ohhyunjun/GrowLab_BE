package com.metaverse.growlab_be.photo.controller;

import com.metaverse.growlab_be.photo.dto.GrowthTrendResponseDto;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.dto.PhotoResponseDto;
import com.metaverse.growlab_be.photo.service.PhotoService;
import com.metaverse.growlab_be.plant.service.PlantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // 생성자 주입을 위한 Lombok 어노테이션
@Slf4j
public class PhotoController {

    private final PhotoService photoService;
    private final PlantService plantService;
    private final com.metaverse.growlab_be.harvest.DeviceEdgeAuth edgeAuth;

    @PostMapping("/photos")
    public ResponseEntity<PhotoResponseDto> uploadPhoto(
            @ModelAttribute PhotoRequestDto requestDto,
            @RequestHeader(value="X-Device-Key",required=false) String deviceKey) throws IOException {

        boolean edge=requestDto.getPlantId()!=null || requestDto.getCapturedAt()!=null || requestDto.getObservationId()!=null || requestDto.getHarvestPredictionJson()!=null;
        if(edge || edgeAuth.isConfigured(requestDto.getSerialNumber())) {
            edgeAuth.verify(requestDto.getSerialNumber(),deviceKey);
            if(!edge) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"기기 등록 후에는 재배 ID와 촬영 시각을 함께 보내야 합니다.");
        }

        // 서비스 호출
        PhotoResponseDto responseDto = photoService.savePhoto(requestDto);

        // 분석 결과를 식물 상태에 반영
        try {
            if(!edge) plantService.applyObservation(
                    responseDto.getDeviceSerialNumber(), requestDto.getPortIndex(),
                    responseDto.getGrowthResult(), responseDto.getConfidence(),
                    responseDto.getDiseaseResult(), responseDto.getDiseaseConfidence());
        } catch (RuntimeException e) {
            // 식물 상태 반영에 실패해도 저장된 사진은 유지
            log.error("사진 저장 후 식물 상태 반영 실패 - photoId: {}, portIndex: {}",
                    responseDto.getId(), requestDto.getPortIndex(), e);
        }
        // 생성 성공 시, 201 CREATED 상태 코드와 생성된 리소스의 DTO를 함께 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/photos/latest")
    public ResponseEntity<PhotoResponseDto> getLatestPhoto() {
        PhotoResponseDto responseDto = photoService.findLatestPhoto();
        return ResponseEntity.ok(responseDto);
    }

    // 프론트 → 포트별 성장 추이(관측 크기) + 또래 비교 조회
    @GetMapping("/photos/growth-trend")
    public ResponseEntity<GrowthTrendResponseDto> getGrowthTrend(
            @RequestParam String serialNumber,
            @RequestParam Integer portIndex) {
        try {
            return ResponseEntity.ok(photoService.getGrowthTrend(serialNumber, portIndex));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
