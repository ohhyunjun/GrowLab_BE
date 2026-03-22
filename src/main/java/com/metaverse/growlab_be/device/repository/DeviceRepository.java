package com.metaverse.growlab_be.device.repository;

import com.metaverse.growlab_be.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String>  {
}
