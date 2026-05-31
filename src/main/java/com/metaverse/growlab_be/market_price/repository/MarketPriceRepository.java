package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    // 1. 특정 품목/품종의 가장 최신 가격 1건
    Optional<MarketPrice> findFirstByItemCodeAndKindCodeOrderByPriceDateDesc(
            String itemCode, String kindCode);

    // 2. 특정 품목/품종의 특정 날짜 이후 가격 내역 (오름차순)
    List<MarketPrice> findByItemCodeAndKindCodeAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            String itemCode, String kindCode, LocalDate startDate);

    // 3. 중복 저장 방지 (rank_code 제거)
    boolean existsByItemCodeAndKindCodeAndPriceDateAndMarketTypeAndRegionCodeAndMarketCode(
            String itemCode,
            String kindCode,
            LocalDate priceDate,
            MarketPrice.MarketType marketType,
            String regionCode,
            String marketCode
    );
}
