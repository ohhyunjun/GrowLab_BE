package com.metaverse.growlab_be.diary.repository;

import com.metaverse.growlab_be.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findAllByOrderByCreatedAtDesc();

    Optional<Diary> findByIdAndPlantId(Long diaryId, Long plantId);

    List<Diary> findAllByPlantIdOrderByCreatedAtDesc(Long plantId);
}
