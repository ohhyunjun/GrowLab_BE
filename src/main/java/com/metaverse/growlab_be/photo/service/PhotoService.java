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
    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto dto) throws IOException {
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(dto.getSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + dto.getSerialNumber()));

        // 파일 저장
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID() + "." + extension;
        String filePath  = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        // 분석 결과 resolve
        String  growthResult  = dto.getGrowthResult()      != null ? dto.getGrowthResult()      : "no_detection";
        Double  growthConf    = dto.getGrowthConfidence()  != null ? dto.getGrowthConfidence()  : 0.0;
        String  diseaseResult = dto.getDiseaseResult()     != null ? dto.getDiseaseResult()     : "no_detection";
        Double  diseaseConf   = dto.getDiseaseConfidence() != null ? dto.getDiseaseConfidence() : 0.0;

        // 한 번에 save
        Photo savedPhoto = photoRepository.save(new Photo(
                device, filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf
        ));

        // Plant 상태 갱신 + 알림
        if (device.getUser() != null && dto.getPortIndex() != null) {
            plantRepository.findByDeviceIdAndPortIndex(device.getId(), dto.getPortIndex())
                    .ifPresent(plant -> {
                        try {
                            updatePlantStageAndNotice(device, plant, growthResult,
                                    diseaseResult, determineCropType(plant));
                        } catch (Exception e) {
                            System.err.println("식물 상태 업데이트 중 오류: " + e.getMessage());
                        }
                    });
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
    private void updatePlantStageAndNotice(Device device, Plant plant,
                                           String growthResult,
                                           String diseaseResult,
                                           String cropType) {

        // 질병 처리 알림
        if ("disease".equalsIgnoreCase(diseaseResult)) {
            noticeService.createAnalysisNotice(device, "식물에 이상이 감지되었습니다. 확인해주세요.",
                    NoticeType.SENSOR_ALERT, 1);
        }

        // 작물별 생육 단계 처리
        if ("lettuce".equalsIgnoreCase(cropType)) {
            handleLettuceStage(device, plant, plant.getPlantStage(), growthResult);
        } else {
            handleTomatoStage(device, plant, plant.getPlantStage(), growthResult);
        }
    }

    private void handleLettuceStage(Device device, Plant plant,
                                    PlantStage currentStage, String growthResult) {
        if ("sprout".equalsIgnoreCase(growthResult) && currentStage == PlantStage.SEED) {
            plant.setPlantStage(PlantStage.GERMINATION);
            if (plant.getGerminatedAt() == null) plant.setGerminatedAt(LocalDateTime.now());
            plantRepository.save(plant);
            noticeService.createAnalysisNotice(device, "새싹이 발아했습니다!", NoticeType.SYSTEM_NOTICE, 2);
        }
        if ("growth".equalsIgnoreCase(growthResult) && currentStage == PlantStage.GERMINATION) {
            plant.setPlantStage(PlantStage.MATURE);
            plant.setMaturedAt(LocalDateTime.now());
            plantRepository.save(plant);
            noticeService.createAnalysisNotice(device, "수확 시기가 되었습니다!", NoticeType.SYSTEM_NOTICE, 1);
        }
    }

    private void handleTomatoStage(Device device, Plant plant,
                                   PlantStage currentStage, String growthResult) {
        if ("sprout".equalsIgnoreCase(growthResult) && currentStage == PlantStage.SEED) {
            plant.setPlantStage(PlantStage.GERMINATION);
            if (plant.getGerminatedAt() == null) plant.setGerminatedAt(LocalDateTime.now());
            plantRepository.save(plant);
            noticeService.createAnalysisNotice(device, "새싹이 발아했습니다!", NoticeType.SYSTEM_NOTICE, 2);
        }
        if (growthResult.toLowerCase().matches("level [1-6]") && currentStage == PlantStage.GERMINATION) {
            plant.setPlantStage(PlantStage.MATURE);
            plant.setMaturedAt(LocalDateTime.now());
            plantRepository.save(plant);
            noticeService.createAnalysisNotice(device, "열매가 발견되었습니다! 수확 시기를 확인하세요.",
                    NoticeType.SYSTEM_NOTICE, 1);
        }
    }

    // ── 헬퍼 메서드 ───────────────────────────────────────────────
    private String determineCropType(Plant plant) {
        String name = plant.getSpecies().getName().toLowerCase();
        if (name.contains("상추") || name.contains("lettuce")) return "lettuce";
        if (name.contains("토마토") || name.contains("tomato")) return "tomato";
        return "tomato";
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