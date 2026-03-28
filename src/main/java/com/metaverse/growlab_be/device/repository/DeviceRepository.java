package com.metaverse.growlab_be.device.repository;

import com.metaverse.growlab_be.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String>  {
}
