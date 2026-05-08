package com.metaverse.growlab_be.photo.service;


import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.notice.domain.NoticeType;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.dto.PhotoResponseDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantStage;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository  photoRepository;
    private final DeviceRepository deviceRepository;
    private final PlantRepository  plantRepository;
    private final NoticeService    noticeService;

    @Value("${file.upload-dir.camera}")
    private String uploadDir;

    // ── 사진 저장 + YOLO 결과 저장 ───────────────────────────────
    // 라즈베리파이가 YOLO 추론 후 사진 + 결과를 같이 전송
    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto requestDto) throws IOException {
        MultipartFile imageFile    = requestDto.getImageFile();
        String        serialNumber = requestDto.getSerialNumber();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

        // 파일 저장
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName  = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID() + "." + extension;
        String filePath  = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        Photo photo      = new Photo(device, filePath, fileName);
        Photo savedPhoto = photoRepository.save(photo);

        // 라즈베리파이에서 YOLO 추론 결과를 같이 전송받아 바로 저장
        String  bestResult    = requestDto.getBestResult()    != null ? requestDto.getBestResult()    : "no_detection";
        Double  avgConfidence = requestDto.getAvgConfidence() != null ? requestDto.getAvgConfidence() : 0.0;
        Integer totalDetected = requestDto.getTotalDetected() != null ? requestDto.getTotalDetected() : 0;
        String  detailedJson  = buildDetailedJson(requestDto.getClassSummary(), requestDto.getDetections());

        savedPhoto.updateDetailedAnalysis(bestResult, avgConfidence, totalDetected, detailedJson);

        // 식물 상태 업데이트 + 알림 생성
        if (device.getUser() != null) {
            try {
                String cropType = determineCropType(device);
                updatePlantStageAndNotice(device, bestResult, requestDto.getClassSummary(), cropType);
            } catch (Exception e) {
                System.err.println("알림 생성 중 오류: " + e.getMessage());
            }
        }

        return new PhotoResponseDto(savedPhoto);
    }

    // ── 최신 사진 조회 ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    // ── 식물 상태 업데이트 + Notice 생성 ─────────────────────────
    private void updatePlantStageAndNotice(Device device, String bestResult,
                                           String classSummaryJson, String cropType) {
        Optional<Plant> plantOpt = plantRepository.findByDeviceId(device.getId());
        if (plantOpt.isEmpty()) return;

        Plant      plant        = plantOpt.get();
        PlantStage currentStage = plant.getPlantStage();

        // classSummary JSON 파싱
        Map<String, Integer> classSummary;
        try {
            classSummary = new ObjectMapper().readValue(
                    classSummaryJson != null ? classSummaryJson : "{}",
                    new TypeReference<Map<String, Integer>>() {}
            );
        } catch (Exception e) {
            classSummary = Map.of();
        }

        if ("lettuce".equalsIgnoreCase(cropType)) {
            // 상추: SEED → GERMINATION → MATURE
            int sproutCount = classSummary.getOrDefault("sprout", 0);
            if (sproutCount > 0 && currentStage == PlantStage.SEED) {
                plant.setPlantStage(PlantStage.GERMINATION);
                if (plant.getGerminatedAt() == null) {
                    plant.setGerminatedAt(LocalDateTime.now());
                }
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(device, "새싹이 발아했습니다!", NoticeType.SYSTEM_NOTICE, 2);
            }

            if ("growth".equalsIgnoreCase(bestResult) && currentStage == PlantStage.GERMINATION) {
                plant.setPlantStage(PlantStage.MATURE);
                plant.setMaturedAt(LocalDateTime.now());
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(device, "수확 시기가 되었습니다!", NoticeType.SYSTEM_NOTICE, 1);
            }

            if ("disease".equalsIgnoreCase(bestResult)) {
                noticeService.createAnalysisNotice(device, "식물에 이상이 감지되었습니다. 확인해주세요.",
                        NoticeType.SENSOR_ALERT, 1);
            }

        } else {
            // 토마토: SEED → GERMINATION → MATURE
            int sproutCount = classSummary.getOrDefault("sprout", 0);
            if (sproutCount > 0 && currentStage == PlantStage.SEED) {
                plant.setPlantStage(PlantStage.GERMINATION);
                if (plant.getGerminatedAt() == null) {
                    plant.setGerminatedAt(LocalDateTime.now());
                }
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(device, "새싹이 발아했습니다!",
                        NoticeType.SYSTEM_NOTICE, 2);
            }

            int fruitCount = 0;
            for (int i = 1; i <= 6; i++) {
                fruitCount += classSummary.getOrDefault("level " + i, 0);
            }
            if (fruitCount > 0 && currentStage == PlantStage.GERMINATION) {
                plant.setPlantStage(PlantStage.MATURE);
                plant.setMaturedAt(LocalDateTime.now());
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(
                        device, "열매가 발견되었습니다! 수확 시기를 확인하세요.",
                        NoticeType.SYSTEM_NOTICE, 1);
            }
        }
    }

    // ── 헬퍼 메서드 ───────────────────────────────────────────────
    private String determineCropType(Device device) {
        try {
            return plantRepository.findByDeviceId(device.getId())
                    .map(plant -> {
                        String name = plant.getSpecies().getName().toLowerCase();
                        if (name.contains("상추") || name.contains("lettuce")) return "lettuce";
                        if (name.contains("토마토") || name.contains("tomato")) return "tomato";
                        return "tomato";
                    })
                    .orElse("tomato");
        } catch (Exception e) {
            return "tomato";
        }
    }

    private String buildDetailedJson(String classSummary, String detections) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of(
                    "classSummary", classSummary != null ? classSummary : "{}",
                    "detections",   detections   != null ? detections   : "[]"
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        try {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }
}