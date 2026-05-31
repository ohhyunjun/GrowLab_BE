package com.metaverse.growlab_be.prediction.repository;

import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.prediction.domain.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    // 특정 식물의 최신 예측 조회
    Optional<Prediction> findTopByPlantOrderByCreatedAtDesc(Plant plant);

    // 기기 삭제 시 연관 예측 삭제
    void deleteByPlant(Plant plant);
}
