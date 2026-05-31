package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {

    boolean existsByRegionCodeAndMarketCode(String regionCode, String marketCode);

    // 거래유형별 지역코드 조회 (가격 수집 시 사용)
    List<RegionCode> findByMarketType(RegionCode.MarketType marketType);
}