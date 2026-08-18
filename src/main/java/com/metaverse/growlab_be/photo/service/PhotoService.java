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
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID() + "." + extension;
        String filePath  = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        String  growthResult  = dto.getGrowthResult()      != null ? dto.getGrowthResult()      : "no_detection";
        Double  growthConf    = dto.getGrowthConfidence()  != null ? dto.getGrowthConfidence()  : 0.0;
        String  diseaseResult = dto.getDiseaseResult()     != null ? dto.getDiseaseResult()     : "no_detection";
        Double  diseaseConf   = dto.getDiseaseConfidence() != null ? dto.getDiseaseConfidence() : 0.0;

        Photo savedPhoto = photoRepository.save(new Photo(
                device, dto.getPortIndex(), filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf
        ));

        if (device.getUser() != null) {
            plantRepository.findByDeviceIdAndPortIndex(device.getId(), dto.getPortIndex())
                    .ifPresent(plant -> {
                        try {
                            updatePlantStageAndNotice(device, plant, dto.getPortIndex(), growthResult, diseaseResult);
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
                                           String growthResult, String diseaseResult) {

        // 1. 질병 상태 업데이트
        if ("disease".equalsIgnoreCase(diseaseResult)) {
            plant.setDiseaseResult(diseaseResult);
            plantRepository.save(plant);
            String message = String.format("[%d번 포트] 식물에 질병이 감지되었습니다. 확인해주세요.", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SENSOR_ALERT, 1);
        } else if ("healthy".equalsIgnoreCase(diseaseResult) || "no_detection".equalsIgnoreCase(diseaseResult)) {
            plant.setDiseaseResult(null);
            plantRepository.save(plant);
        }

        // ✅ 2. 생육 단계 처리 - 품종별 단계 수(가변)를 지원하는 범용 로직
        Species species = device.getSpecies();
        if (species == null) return;

        Integer targetIndex = resolveTargetStageIndex(growthResult, species);
        if (targetIndex == null) return; // 인식 불가("no_detection" 등) → 단계 유지

        int currentIndex = plant.getStageIndex() != null ? plant.getStageIndex() : 0;
        int maxIndex = species.getStageCount() - 1;
        int safeTarget = Math.max(0, Math.min(targetIndex, maxIndex));

        // 단계는 뒤로 가지 않음 (오탐지로 인한 후퇴 방지)
        if (safeTarget <= currentIndex) return;

        plant.setStageIndex(safeTarget);

        // 첫 단계(0)를 벗어나는 순간을 "발아 시점"으로 기록
        if (currentIndex == 0 && plant.getGerminatedAt() == null) {
            plant.setGerminatedAt(LocalDateTime.now());
        }
        // 마지막 단계에 도달하면 "수확(성숙) 시점" 기록
        if (safeTarget == maxIndex && plant.getMaturedAt() == null) {
            plant.setMaturedAt(LocalDateTime.now());
        }

        plantRepository.save(plant);

        String stageName = species.getStageName(safeTarget);
        String message = safeTarget == maxIndex
                ? String.format("[%d번 포트] %s 단계에 도달했습니다! 수확 시기를 확인하세요.", portIndex, stageName)
                : String.format("[%d번 포트] %s 단계로 전환되었습니다!", portIndex, stageName);
        int priority = safeTarget == maxIndex ? 1 : 2;
        noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, priority);
    }

    // ✅ Vision AI가 준 growthResult를 이 품종의 단계 인덱스로 해석
    // 1) 숫자 문자열이면 그대로 인덱스로 사용 (예: "3")
    // 2) 품종의 stageNames 중 이름이 일치하면 그 인덱스 사용 (예: "착과")
    // 3) 둘 다 아니면(예: "no_detection") null 반환하여 단계 변경 안 함
    private Integer resolveTargetStageIndex(String growthResult, Species species) {
        if (growthResult == null || growthResult.isBlank()) return null;
        String trimmed = growthResult.trim();

        if (trimmed.equalsIgnoreCase("no_detection")) return null;

        // 1) 숫자로 온 경우
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            // 숫자가 아니면 아래에서 이름으로 매칭 시도
        }

        // 2) 단계 이름으로 온 경우
        List<String> stageNames = species.getStageNames();
        if (stageNames != null) {
            for (int i = 0; i < stageNames.size(); i++) {
                if (stageNames.get(i).equalsIgnoreCase(trimmed)) {
                    return i;
                }
            }
        }

        // 3) 레거시 호환: 기존 상추/토마토 모델이 쓰던 라벨("sprout","growth")도 계속 인식
        if (trimmed.equalsIgnoreCase("sprout")) return 1;
        if (trimmed.equalsIgnoreCase("growth")) return species.getStageCount() - 1;

        return null; // 알 수 없는 라벨은 무시
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