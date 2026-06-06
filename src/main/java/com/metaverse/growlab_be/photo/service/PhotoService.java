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

    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto dto) throws IOException {
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(dto.getSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + dto.getSerialNumber()));

        if (dto.getPortIndex() == null) {
            throw new IllegalArgumentException("포트 번호(portIndex)가 누락되었습니다.");
        }

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

        // Photo 엔티티 생성 시 portIndex 추가 반영
        Photo savedPhoto = photoRepository.save(new Photo(
                device, dto.getPortIndex(), filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf
        ));

        // Plant 상태 갱신 + 알림
        if (device.getUser() != null) {
            plantRepository.findByDeviceIdAndPortIndex(device.getId(), dto.getPortIndex())
                    .ifPresent(plant -> {
                        try {
                            updatePlantStageAndNotice(device, plant, dto.getPortIndex(), growthResult,
                                    diseaseResult, determineCropType(plant));
                        } catch (Exception e) {
                            System.err.println("식물 상태 업데이트 중 오류: " + e.getMessage());
                        }
                    });
        }

        return new PhotoResponseDto(savedPhoto);
    }

    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    private void updatePlantStageAndNotice(Device device, Plant plant, Integer portIndex,
                                           String growthResult, String diseaseResult, String cropType) {

        // 1. 질병 상태 업데이트 로직 추가
        if ("disease".equalsIgnoreCase(diseaseResult)) {
            plant.setDiseaseResult(diseaseResult);
            plantRepository.save(plant); // 식물 DB에 질병 상태 반영
            String message = String.format("[%d번 포트] 식물에 질병이 감지되었습니다. 확인해주세요.", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SENSOR_ALERT, 1);
        } else if ("healthy".equalsIgnoreCase(diseaseResult) || "no_detection".equalsIgnoreCase(diseaseResult)) {
            plant.setDiseaseResult(null); // 건강할 경우 질병 상태 초기화
            plantRepository.save(plant);
        }

        // 2. 작물별 생육 단계 처리
        if ("lettuce".equalsIgnoreCase(cropType)) {
            handleLettuceStage(device, plant, portIndex, plant.getPlantStage(), growthResult);
        } else {
            handleTomatoStage(device, plant, portIndex, plant.getPlantStage(), growthResult);
        }
    }

    private void handleLettuceStage(Device device, Plant plant, Integer portIndex,
                                    PlantStage currentStage, String growthResult) {
        if ("sprout".equalsIgnoreCase(growthResult) && currentStage == PlantStage.SEED) {
            plant.setPlantStage(PlantStage.GERMINATION);
            if (plant.getGerminatedAt() == null) plant.setGerminatedAt(LocalDateTime.now());
            plantRepository.save(plant);

            String message = String.format("[%d번 포트] 새싹이 발아했습니다!", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, 2);
        }
        if ("growth".equalsIgnoreCase(growthResult) && currentStage == PlantStage.GERMINATION) {
            plant.setPlantStage(PlantStage.MATURE);
            plant.setMaturedAt(LocalDateTime.now());
            plantRepository.save(plant);

            String message = String.format("[%d번 포트] 수확 시기가 되었습니다!", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, 1);
        }
    }

    private void handleTomatoStage(Device device, Plant plant, Integer portIndex,
                                   PlantStage currentStage, String growthResult) {
        if ("sprout".equalsIgnoreCase(growthResult) && currentStage == PlantStage.SEED) {
            plant.setPlantStage(PlantStage.GERMINATION);
            if (plant.getGerminatedAt() == null) plant.setGerminatedAt(LocalDateTime.now());
            plantRepository.save(plant);

            String message = String.format("[%d번 포트] 새싹이 발아했습니다!", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, 2);
        }
        if (growthResult.toLowerCase().matches("level [1-6]") && currentStage == PlantStage.GERMINATION) {
            plant.setPlantStage(PlantStage.MATURE);
            plant.setMaturedAt(LocalDateTime.now());
            plantRepository.save(plant);

            String message = String.format("[%d번 포트] 열매가 발견되었습니다! 수확 시기를 확인하세요.", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, 1);
        }
    }

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