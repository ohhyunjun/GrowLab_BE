package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    // ─── 최신 가격 1건 (marketType 포함) ────────────────────
    Optional<MarketPrice> findFirstByItemCodeAndKindCodeAndMarketTypeOrderByPriceDateDesc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType);

    // 실제 가격이 존재하는 최근 거래일을 최신순으로 조회한다.
    @Query("""
            select distinct mp.priceDate
            from MarketPrice mp
            where mp.itemCode = :itemCode
              and mp.kindCode = :kindCode
              and mp.marketType = :marketType
            order by mp.priceDate desc
            """)
    List<LocalDate> findLatestDistinctPriceDates(
            @Param("itemCode") String itemCode,
            @Param("kindCode") String kindCode,
            @Param("marketType") MarketPrice.MarketType marketType,
            Pageable pageable);

    // 선택된 거래일의 모든 시장 가격을 반환한다. FE가 날짜별 평균을 계산한다.
    List<MarketPrice> findByItemCodeAndKindCodeAndMarketTypeAndPriceDateInOrderByPriceDateAsc(
            String itemCode, String kindCode, MarketPrice.MarketType marketType,
            Collection<LocalDate> priceDates);

    // ─── 중복 체크 ───────────────────────────────────────────
    boolean existsByItemCodeAndKindCodeAndPriceDateAndMarketTypeAndRegionCodeAndMarketCode(
            String itemCode, String kindCode, LocalDate priceDate,
            MarketPrice.MarketType marketType, String regionCode, String marketCode);
}
