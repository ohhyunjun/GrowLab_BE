package com.metaverse.growlab_be.diary.repository;

import com.metaverse.growlab_be.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
}
