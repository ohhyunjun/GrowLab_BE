package com.metaverse.growlab_be.sensor_log.repository;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.sensor_log.domain.SensorAnomaly;
import com.metaverse.growlab_be.sensor_log.domain.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorAnomalyRepository extends JpaRepository<SensorAnomaly,Long> {
    Optional<SensorAnomaly> findTopByDeviceAndSensorTypeAndEndedAtIsNull(
            Device device, SensorType sensorType);

    void deleteByDevice(Device device);
}
