package com.metaverse.growlab_be.plant.repository;

import com.metaverse.growlab_be.plant.domain.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant,Long> {

    // Device를 통한 간접 사용자 조회
    @Query("SELECT p FROM Plant p WHERE p.device.user.id = :userId ORDER BY p.plantedAt ASC")
    List<Plant> findByUserIdOrderByPlantedAtAsc(@Param("userId") Long userId);

    // Device를 통한 간접 소유권 확인
    @Query("SELECT p FROM Plant p WHERE p.id = :plantId AND p.device.user.id = :userId")
    Optional<Plant> findByIdAndUserId(@Param("plantId") Long plantId, @Param("userId") Long userId);

    // 디바이스 중복 등록 확인(포트번호 확인)
    boolean existsByDeviceIdAndPortIndex(String deviceId, Integer portIndex);

    boolean existsByDeviceId(String deviceId);

    // 디바이스로 식물 조회
    Optional<Plant> findByDeviceIdAndPortIndex(String deviceSerial, Integer portIndex);

    // ✅ 특정 생육 단계(인덱스)에 있는 모든 식물 조회 (품종마다 단계 수가 달라 PlantStage enum 대신 stageIndex 사용)
    List<Plant> findByStageIndex(Integer stageIndex);
}