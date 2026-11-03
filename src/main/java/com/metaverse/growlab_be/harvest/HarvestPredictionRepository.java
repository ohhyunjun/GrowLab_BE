package com.metaverse.growlab_be.harvest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface HarvestPredictionRepository extends JpaRepository<HarvestPrediction,Long> {
    Optional<HarvestPrediction> findTopByPlantIdOrderByCapturedAtDesc(Long plantId);
    @Query("select h from HarvestPrediction h where h.status = 'FEATURES_READY' and h.plant.endedAt is null order by h.receivedAt asc")
    List<HarvestPrediction> findPendingActive(Pageable page);
}
