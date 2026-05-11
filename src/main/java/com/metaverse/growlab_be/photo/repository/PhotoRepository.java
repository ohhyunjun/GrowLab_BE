package com.metaverse.growlab_be.photo.repository;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.photo.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    Optional<Photo> findTopByOrderByIdDesc();// JpaRepository<관리할 엔티티, 엔티티의 ID 타입>

    //디바이스별 사진 조회 (최신순)
    List<Photo> findByDeviceOrderByCreatedAtDesc(Device device);

    Optional<Photo> findTopByDeviceIdOrderByCreatedAtDesc(String deviceId);
}