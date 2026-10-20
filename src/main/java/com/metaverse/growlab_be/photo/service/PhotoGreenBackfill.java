package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** One-time, opt-in measurement of stored camera photos for historical comparisons. */
@Component
@ConditionalOnProperty(name = "growth.green-backfill", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class PhotoGreenBackfill implements ApplicationRunner {
    private final PhotoRepository photos;

    @Value("${growth.green-backfill-limit:1000}")
    private int limit;

    @Value("${growth.camera-change-serial:GROWLAB-G111}")
    private String cameraChangeSerial;

    @Value("${growth.camera-change-at:2026-10-19T18:00:00}")
    private String cameraChangeAt;

    @Override
    public void run(ApplicationArguments args) {
        int measured = 0;
        int pageNumber = 0;
        Page<Photo> page;
        do {
            page = photos.findAll(PageRequest.of(pageNumber++, 100, Sort.by("id")));
            for (Photo photo : page.getContent()) {
                if (limit > 0 && measured >= limit) {
                    log.info("Green backfill stopped at configured limit {}", limit);
                    return;
                }
                boolean wideCamera = isWideCamera(photo);
                double minConfidence = PhotoService.minimumGrowthConfidence(wideCamera);
                if (photo.getGreenAreaRatio() != null || photo.getConfidence() == null
                        || photo.getConfidence() < minConfidence
                        || "no_detection".equalsIgnoreCase(photo.getGrowthStageResult())) continue;
                PlantExtent.Result extent = PlantExtent.of(photo.getDetectionsJson(), wideCamera);
                if (extent == null || extent.confidence() < minConfidence) continue;
                String filePath = photo.getFilePath();
                if (filePath == null || filePath.isBlank() || filePath.startsWith("https://")) continue;
                try (var input = Files.newInputStream(Path.of(filePath))) {
                    Double ratio = GreenCoverage.frameRatio(input, extent);
                    if (ratio == null) continue;
                    photo.recordGreenAreaRatio(ratio);
                    photos.save(photo);
                    measured++;
                } catch (Exception ex) {
                    log.warn("Green backfill skipped photo {}", photo.getId(), ex);
                }
            }
        } while (page.hasNext());
        log.info("Green backfill completed: {} photos measured", measured);
    }
    private boolean isWideCamera(Photo photo) {
        if (cameraChangeSerial == null || cameraChangeSerial.isBlank()
                || cameraChangeAt == null || cameraChangeAt.isBlank()
                || !cameraChangeSerial.equals(photo.getDevice().getId())) return false;
        LocalDateTime shot = photo.getCapturedAt() == null ? photo.getCreatedAt()
                : photo.getCapturedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        return shot != null && !shot.isBefore(LocalDateTime.parse(cameraChangeAt));
    }
}
