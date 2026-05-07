package com.metaverse.growlab_be.sensor_log.repository;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.sensor_log.domain.SensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long>  {
    Optional<SensorLog> findTopBySerialNumberOrderByCreatedAtDesc(Device device);
}
