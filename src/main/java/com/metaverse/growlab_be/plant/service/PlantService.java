package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.notice.domain.NoticeType;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantService {

    private final PlantRepository plantRepository;
    private final DeviceRepository deviceRepository;
    private final NoticeService noticeService;

    private static final double DISEASE_MIN_CONFIDENCE = 0.60;
    private static final double GROWTH_STAGE_MIN_CONFIDENCE = 0.70;

    @Transactional
    public PlantResponseDto createPlant(PlantRequestDto plantRequestDto, User user) {
        Device device = findDeviceOwnedByUser(plantRequestDto.getSerialNumber(), user);

        if (plantRepository.existsByDeviceIdAndPortIndex(device.getId(), plantRequestDto.getPortIndex())) {
            throw new IllegalArgumentException(plantRequestDto.getPortIndex() + "번 포트에는 이미 식물이 등록되어 있습니다.");
        }

        if (device.getSpecies() == null) {
            throw new IllegalArgumentException("기기에 재배 품종을 먼저 설정해야 합니다.");
        }

        Plant plant = new Plant(plantRequestDto, device);
        // ✅ 요청된 단계가 이 품종의 단계 범위를 벗어나면 안전하게 보정
        plant.setStageIndex(clampStageIndex(device, plantRequestDto.getStageIndex()));

        Plant savedPlant = plantRepository.save(plant);
        return new PlantResponseDto(savedPlant);
    }

    public List<PlantResponseDto> getPlants(User user) {
        List<Plant> plants = plantRepository.findByUserIdOrderByPlantedAtAsc(user.getId());
        return plants.stream()
                .map(PlantResponseDto::new)
                .toList();
    }

    public PlantResponseDto getPlantById(Long plantId, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        return new PlantResponseDto(plant);
    }

    @Transactional
    public PlantResponseDto updatePlant(Long plantId, PlantRequestDto plantRequestDto, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        plant.update(plantRequestDto);
        plant.setStageIndex(clampStageIndex(plant.getDevice(), plantRequestDto.getStageIndex()));
        return new PlantResponseDto(plant);
    }

    @Transactional
    public PlantResponseDto patchPlant(Long plantId, PlantRequestDto plantRequestDto, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);

        if (plantRequestDto.getName() != null) plant.setName(plantRequestDto.getName());
        if (plantRequestDto.getStageIndex() != null) {
            plant.setStageIndex(clampStageIndex(plant.getDevice(), plantRequestDto.getStageIndex()));
        }
        if (plantRequestDto.getPlantedAt() != null) plant.setPlantedAt(plantRequestDto.getPlantedAt());

        return new PlantResponseDto(plant);
    }

    @Transactional
    public void deletePlant(Long plantId, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        plantRepository.delete(plant);
    }

    // 관측 결과를 해당 기기·포트의 식물 상태에 반영
    @Transactional
    public void applyObservation(String serialNumber, Integer portIndex,
                                 String growthResult, Double growthConfidence,
                                 String diseaseResult, Double diseaseConfidence) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

        if (device.getUser() == null) return;

        Optional<Plant> optionalPlant = plantRepository.findByDeviceIdAndPortIndex(device.getId(), portIndex);

        if (optionalPlant.isPresent()) {
            Plant plant = optionalPlant.get();
            updatePlantStageAndNotice(device, plant, portIndex,
                    growthResult, growthConfidence, diseaseResult, diseaseConfidence);
        }
    }

    private void updatePlantStageAndNotice(Device device, Plant plant, Integer portIndex,
                                           String growthResult, Double growthConfidence,
                                           String diseaseResult, Double diseaseConfidence) {

        // 1. 질병 상태 업데이트
        boolean diseaseDetected = diseaseResult != null
                && !"no_detection".equalsIgnoreCase(diseaseResult)
                && !"healthy".equalsIgnoreCase(diseaseResult);

        if (diseaseDetected
                && diseaseConfidence != null
                && diseaseConfidence >= DISEASE_MIN_CONFIDENCE) {
            plant.setDiseaseResult(diseaseResult);
            plantRepository.save(plant);

            String message = String.format("[%d번 포트] 식물에 질병이 감지되었습니다. 확인해주세요.", portIndex);
            noticeService.createAnalysisNotice(device, message, NoticeType.SENSOR_ALERT, 1);

        } else if ("healthy".equalsIgnoreCase(diseaseResult)) {
            plant.setDiseaseResult(null);
            plantRepository.save(plant);
        }
        // no_detection, null, 신뢰도 미달은 기존 질병 상태 유지

        // 2. 품종별 생육 단계 처리
        Species species = device.getSpecies();
        if (species == null) return;

        if (growthConfidence == null || growthConfidence < GROWTH_STAGE_MIN_CONFIDENCE) return;

        Integer targetIndex = resolveTargetStageIndex(growthResult);
        if (targetIndex == null) return;

        int currentIndex = plant.getStageIndex() != null ? plant.getStageIndex() : 0;
        int maxIndex = species.getStageCount() - 1;
        int safeTarget = Math.max(0, Math.min(targetIndex, maxIndex));

        // 단계는 뒤로 가지 않음
        if (safeTarget <= currentIndex) return;

        plant.setStageIndex(safeTarget);

        // 첫 단계(0)를 벗어나는 순간을 발아 시점으로 기록
        if (currentIndex == 0 && plant.getGerminatedAt() == null) {
            plant.setGerminatedAt(LocalDateTime.now());
        }

        // 마지막 단계에 도달하면 성숙 시점 기록
        if (safeTarget == maxIndex && plant.getMaturedAt() == null) {
            plant.setMaturedAt(LocalDateTime.now());
        }

        plantRepository.save(plant);

        // 3. 상태 변경 알림 요청
        String stageName = species.getStageName(safeTarget);
        String message = safeTarget == maxIndex
                ? String.format("[%d번 포트] %s 단계에 도달했습니다! 수확 시기를 확인하세요.", portIndex, stageName)
                : String.format("[%d번 포트] %s 단계로 전환되었습니다!", portIndex, stageName);
        int priority = safeTarget == maxIndex ? 1 : 2;

        noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, priority);
    }

    // 숫자 인덱스를 우선하고, 문자열 라벨은 정식=1/생육=2/수확=3으로 고정한다.
    private Integer resolveTargetStageIndex(String growthResult) {
        if (growthResult == null || growthResult.isBlank()) return null;
        String trimmed = growthResult.trim();

        if (trimmed.equalsIgnoreCase("no_detection")) return null;

        // 1. 숫자로 온 경우
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            // 숫자가 아니면 아래의 고정 라벨 매핑을 사용한다.
        }

        // 2. 기존 모델 및 한글/영문 라벨 호환
        if (trimmed.equalsIgnoreCase("정식기") || trimmed.equalsIgnoreCase("Planting") || trimmed.equalsIgnoreCase("sprout")) return 1;
        if (trimmed.equalsIgnoreCase("생육기") || trimmed.equalsIgnoreCase("Growing") || trimmed.equalsIgnoreCase("growth")) return 2;
        if (trimmed.equalsIgnoreCase("수확기") || trimmed.equalsIgnoreCase("Harvest")) return 3;

        return null;
    }

    // ✅ 요청된 단계 인덱스를 이 기기의 대표 품종이 가진 단계 범위(0 ~ stageCount-1) 안으로 보정
    private int clampStageIndex(Device device, Integer requested) {
        if (requested == null) return 0;
        int maxIndex = (device != null && device.getSpecies() != null
                ? device.getSpecies().getStageCount()
                : 3) - 1;
        return Math.max(0, Math.min(requested, maxIndex));
    }

    private Plant findPlantOwnedByUser(Long plantId, User user) {
        return plantRepository.findByIdAndUserId(plantId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 식물을 찾을 수 없거나 접근 권한이 없습니다."));
    }

    private Device findDeviceOwnedByUser(String deviceSerial, User user) {
        Device device = deviceRepository.findById(deviceSerial)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 디바이스입니다: " + deviceSerial));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 디바이스에 대한 권한이 없습니다.");
        }
        return device;
    }
}
