package com.metaverse.growlab_be.device.repository;

import com.metaverse.growlab_be.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String>  {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Device d WHERE d.id = :id")
    java.util.Optional<Device> findLockedById(@org.springframework.data.repository.query.Param("id") String id);

    List<Device> findByUserId(Long userId);

}
