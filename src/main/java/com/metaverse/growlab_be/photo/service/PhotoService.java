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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final ObjectMapper objectMapper;

    private final PhotoRepository   photoRepository;
    private final DeviceRepository  deviceRepository;
    private final PlantRepository   plantRepository;
    private final NoticeService noticeService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${file.upload-dir.camera}")
    private String uploadDir;

    @Value("${ai.server.detailed.url}")
    private String aiDetailedServerUrl;

    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto requestDto) throws IOException {
        MultipartFile imageFile   = requestDto.getImageFile();
        String        serialNumber = requestDto.getSerialNumber();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

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

        // AI 상세 분석
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(filePath)));

            String cropType = determineCropType(device);
            body.add("crop_type", cropType);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> detailedResponse = restTemplate.postForObject(
                    aiDetailedServerUrl, entity, Map.class);

            if (detailedResponse != null) {
                String  bestResult     = (String) detailedResponse.getOrDefault("bestResult", "no_detection");
                Integer totalDetected  = Integer.valueOf(detailedResponse.getOrDefault("totalDetected", 0).toString());
                Double  avgConfidence  = Double.valueOf(detailedResponse.getOrDefault("avgConfidence", 0.0).toString());
                String  detailedJson   = convertToJson(detailedResponse);

                savedPhoto.updateDetailedAnalysis(bestResult, avgConfidence, totalDetected, detailedJson);

                // 알림 생성 + 식물 상태 업데이트
                if (device.getUser() != null) {
                    try {
                        updatePlantStageAndNotice(device, detailedResponse, cropType);
                    } catch (Exception e) {
                        System.err.println("알림 생성 중 오류: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("AI 분석 서버 호출 실패: " + e.getMessage());
            savedPhoto.updateDetailedAnalysis("analysis_failed", 0.0, 0, "{}");
        }

        return new PhotoResponseDto(savedPhoto);
    }

    // ── 식물 상태 업데이트 + Notice 생성 ────────────────────────
    @SuppressWarnings("unchecked")
    private void updatePlantStageAndNotice(Device device,
                                           Map<String, Object> response,
                                           String cropType) {
        Optional<Plant> plantOpt = plantRepository.findByDeviceId(device.getId());
        if (plantOpt.isEmpty()) return;

        Plant      plant        = plantOpt.get();
        PlantStage currentStage = plant.getPlantStage();

        Map<String, Integer> classSummary =
                (Map<String, Integer>) response.getOrDefault("classSummary", Map.of());

        if ("lettuce".equalsIgnoreCase(cropType)) {
            // 상추: SEED → GERMINATION → MATURE
            String analysisStage = (String) response.getOrDefault("analysis_stage", "");

            int sproutCount = classSummary.getOrDefault("sprout", 0);
            if (sproutCount > 0 && currentStage == PlantStage.SEED) {
                plant.setPlantStage(PlantStage.GERMINATION);
                if (plant.getGerminatedAt() == null) {
                    plant.setGerminatedAt(LocalDateTime.now());
                }
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(device, "새싹이 발아했습니다!", NoticeType.SYSTEM_NOTICE, 2);
            }

            if ("growth".equalsIgnoreCase(analysisStage) && currentStage == PlantStage.GERMINATION) {
                plant.setPlantStage(PlantStage.MATURE);
                plant.setMaturedAt(LocalDateTime.now());
                plantRepository.save(plant);
                noticeService.createAnalysisNotice(device, "수확 시기가 되었습니다!", NoticeType.SYSTEM_NOTICE, 1);
            }

            // 질병 감지
            if ("disease".equalsIgnoreCase(analysisStage)) {
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

    // ── Notice 생성 헬퍼 ─────────────────────────────────────────
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

    @Transactional
    public PhotoResponseDto analyzePhotoDetailed(PhotoRequestDto requestDto) throws IOException {
        MultipartFile imageFile = requestDto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension    = getFileExtension(imageFile.getOriginalFilename());
        String tempFileName = "temp_" + System.currentTimeMillis() + "." + extension;
        String tempFilePath = Paths.get(uploadDir, tempFileName).toString();
        imageFile.transferTo(new File(tempFilePath));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(tempFilePath)));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> detailedResponse = restTemplate.postForObject(
                    aiDetailedServerUrl, entity, Map.class);

            new File(tempFilePath).delete();

            if (detailedResponse != null) {
                String  bestResult    = (String) detailedResponse.getOrDefault("bestResult", "no_detection");
                Double  avgConfidence = Double.valueOf(detailedResponse.getOrDefault("avgConfidence", 0.0).toString());
                Integer totalDetected = Integer.valueOf(detailedResponse.getOrDefault("totalDetected", 0).toString());

                // DB 저장 없이 임시 Photo 객체로 반환
                Photo tempPhoto = new Photo(null, "", "temp_analysis");
                tempPhoto.updateDetailedAnalysis(bestResult, avgConfidence, totalDetected,
                        convertToJson(detailedResponse));
                return new PhotoResponseDto(tempPhoto);
            }

            throw new RuntimeException("AI 서버로부터 응답을 받지 못했습니다.");

        } catch (Exception e) {
            new File(tempFilePath).delete();
            throw new RuntimeException("상세 분석 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    private String convertToJson(Map<String, Object> response) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "classSummary", response.getOrDefault("classSummary", Map.of()),
                    "detections",   response.getOrDefault("detections",   List.of())
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