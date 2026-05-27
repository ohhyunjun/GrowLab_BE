package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.RankCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankCodeRepository extends JpaRepository<RankCode, Long> {

    // 중복 저장 방지
    boolean existsByCode(String code);
}
