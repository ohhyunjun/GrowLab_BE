package com.metaverse.growlab_be.species.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.notice.domain.NoticeType;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// ✅ 카메라(Vision AI) 분석과 별개로, 날짜 경과만으로 생육 단계를 자동 진행시키는 스케줄러
// 카메라가 먼저 단계를 올렸으면 이미 더 앞서있으니 건드리지 않고,
// 카메라가 못 올렸어도 등록된 날짜가 지나면 자동으로 다음 단계로 넘어가게 함(둘 중 더 앞선 쪽을 유지, 후퇴 없음)
@Service
@RequiredArgsConstructor
public class PlantAutoStageService {

    private final PlantRepository plantRepository;
    private final NoticeService noticeService;

    // 매일 자정 5분에 실행 (다른 스케줄과 겹치지 않게 살짝 늦춤)
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void advanceStagesByDate() {
        List<Plant> allPlants = plantRepository.findAll();

        for (Plant plant : allPlants) {
            try {
                advanceIfDue(plant);
            } catch (Exception e) {
                System.err.println("[PlantAutoStageService] 식물 ID " + plant.getId() + " 자동 단계 갱신 실패: " + e.getMessage());
            }
        }
    }

    private void advanceIfDue(Plant plant) {
        Device device = plant.getDevice();
        if (device == null || device.getSpecies() == null) return;

        Species species = device.getSpecies();
        if (plant.getPlantedAt() == null) return;

        int daysElapsed = (int) ChronoUnit.DAYS.between(
                plant.getPlantedAt().toLocalDate(),
                LocalDateTime.now().toLocalDate()
        );
        if (daysElapsed < 0) return;

        int currentIndex = plant.getStageIndex() != null ? plant.getStageIndex() : 0;
        int dateBasedIndex = species.resolveStageIndexByDays(daysElapsed);
        int maxIndex = species.getStageCount() - 1;
        int safeTarget = Math.max(0, Math.min(dateBasedIndex, maxIndex));

        // 날짜 기준 단계가 현재 단계보다 앞서 있을 때만 진행 (카메라가 이미 더 앞서 있으면 그대로 둠)
        if (safeTarget <= currentIndex) return;

        plant.setStageIndex(safeTarget);

        if (currentIndex == 0 && plant.getGerminatedAt() == null) {
            plant.setGerminatedAt(LocalDateTime.now());
        }
        if (safeTarget == maxIndex && plant.getMaturedAt() == null) {
            plant.setMaturedAt(LocalDateTime.now());
        }

        plantRepository.save(plant);

        String stageName = species.getStageName(safeTarget);
        String message = safeTarget == maxIndex
                ? String.format("[%d번 포트] 등록된 재배 일정에 따라 %s 단계에 도달했습니다.", plant.getPortIndex(), stageName)
                : String.format("[%d번 포트] 등록된 재배 일정에 따라 %s 단계로 전환되었습니다.", plant.getPortIndex(), stageName);
        int priority = safeTarget == maxIndex ? 1 : 3;
        noticeService.createAnalysisNotice(device, message, NoticeType.SYSTEM_NOTICE, priority);
    }
}
