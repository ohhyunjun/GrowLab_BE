package com.metaverse.growlab_be.diary.repository;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 특정 유저가 '특정 식물'에 대해 작성한 일기 목록 조회
    List<Diary> findByUserAndPlantIdOrderByCreatedAtDesc(User user, Long plantId);

    // 특정 유저가 작성한 '모든 식물'의 일기 목록 조회
    List<Diary> findAllByUserOrderByCreatedAtDesc(User user);

    // 특정 식물에 속한 다이어리인지 확인
    Optional<Diary> findByIdAndPlantId(Long id, Long plantId);

    // [보안 핵심] 특정 유저가 작성한 다이어리인지 확인
    Optional<Diary> findByIdAndUser(Long id, User user);

    // 특정 날짜(targetDate)에 작성한 모든 일기 조회
    List<Diary> findByUserAndTargetDate(User user, LocalDate targetDate);
}
