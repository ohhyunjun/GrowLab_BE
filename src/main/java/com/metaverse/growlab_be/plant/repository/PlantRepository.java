package com.metaverse.growlab_be.plant.repository;

import com.metaverse.growlab_be.plant.domain.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant,Long> {

    // Device를 통한 간접 사용자 조회
    @Query("SELECT p FROM Plant p WHERE p.device.user.id = :userId AND p.endedAt IS NULL ORDER BY p.plantedAt ASC")
    List<Plant> findByUserIdOrderByPlantedAtAsc(@Param("userId") Long userId);

    // Device를 통한 간접 소유권 확인
    @Query("SELECT p FROM Plant p WHERE p.id = :plantId AND p.device.user.id = :userId AND p.endedAt IS NULL")
    Optional<Plant> findByIdAndUserId(@Param("plantId") Long plantId, @Param("userId") Long userId);

    // 디바이스 중복 등록 확인(포트번호 확인)
    boolean existsByDeviceIdAndPortIndexAndEndedAtIsNull(String deviceId, Integer portIndex);

    boolean existsByDeviceIdAndEndedAtIsNull(String deviceId);

    boolean existsBySpecies_Id(Long speciesId);

    // 디바이스로 식물 조회
    Optional<Plant> findByDeviceIdAndPortIndexAndEndedAtIsNull(String deviceSerial, Integer portIndex);

    // 기기에 등록된 모든 식물(= 사용 중인 포트). 성장 추이의 또래 비교에서 빈 포트를 걸러내는 데 쓴다.
    List<Plant> findByDeviceIdAndEndedAtIsNull(String deviceSerial);
    // 기기에서 재배가 끝난 식물. 성장 추이의 품종 기준을 만드는 과거 기록으로 쓴다.
    List<Plant> findByDeviceIdAndEndedAtIsNotNull(String deviceSerial);

    // ✅ 특정 생육 단계(인덱스)에 있는 모든 식물 조회 (품종마다 단계 수가 달라 PlantStage enum 대신 stageIndex 사용)
    List<Plant> findByStageIndexAndEndedAtIsNull(Integer stageIndex);

    List<Plant> findByEndedAtIsNull();
}
