package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    Optional<MarketPrice> findFirstByItemCodeAndKindCodeAndMarketTypeOrderByPriceDateDesc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType);

    List<MarketPrice> findByItemCodeAndKindCodeAndMarketTypeAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType, LocalDate startDate);

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
