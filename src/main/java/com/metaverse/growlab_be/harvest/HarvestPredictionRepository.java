package com.metaverse.growlab_be.harvest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HarvestPredictionRepository extends JpaRepository<HarvestPrediction,Long> {
    Optional<HarvestPrediction> findTopByPlantIdOrderByCapturedAtDesc(Long plantId);
}
