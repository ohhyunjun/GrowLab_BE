package com.metaverse.growlab_be.diary.repository;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByUserAndDeviceIdOrderByTargetDateDescCreatedAtDesc(User user, String deviceSerial);
    Optional<Diary> findByIdAndUserAndDeviceId(Long id, User user, String deviceSerial);
    void deleteByDevice(Device device);
    boolean existsByPlantId(Long plantId);
}
