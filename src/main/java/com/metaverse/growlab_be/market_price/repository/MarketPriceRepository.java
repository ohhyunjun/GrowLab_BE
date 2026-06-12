package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    // ─── 최신 가격 1건 (marketType 포함) ────────────────────
    Optional<MarketPrice> findFirstByItemCodeAndKindCodeAndMarketTypeOrderByPriceDateDesc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType);

    // ─── 7일 가격 내역 (marketType 포함) ────────────────────
    List<MarketPrice> findByItemCodeAndKindCodeAndMarketTypeAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType, LocalDate startDate);

    // ─── 중복 체크 ───────────────────────────────────────────
    boolean existsByItemCodeAndKindCodeAndPriceDateAndMarketTypeAndRegionCodeAndMarketCode(
            String itemCode, String kindCode, LocalDate priceDate,
            MarketPrice.MarketType marketType, String regionCode, String marketCode);
}
