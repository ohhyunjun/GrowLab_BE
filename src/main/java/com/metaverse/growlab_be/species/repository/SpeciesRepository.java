package com.metaverse.growlab_be.species.repository;

import com.metaverse.growlab_be.species.domain.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeciesRepository extends JpaRepository<Species, Long> {

    // 1. 최신순으로 모든 품종 목록 가져오기
    List<Species> findAllByOrderByCreatedAtDesc();

    // 2. 품종 이름으로 상세 정보 찾기 (등록 시 중복 확인용)
    Optional<Species> findByName(String name);
}
