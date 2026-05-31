package com.metaverse.growlab_be.market_price.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "market_price",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_market_price",
                        columnNames = {"item_code", "kind_code", "price_date",
                                "market_type", "region_code", "market_code"}
                )
        }
)

public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 품목코드
    @Column(name = "item_code", nullable = false, length = 10)
    private String itemCode;

    // 품목명
    @Column(name = "item_name", nullable = false, length = 50)
    private String itemName;

    // 품종코드
    @Column(name = "kind_code", nullable = false, length = 10)
    private String kindCode;

    // 품종명
    @Column(name = "kind_name", length = 50)
    private String kindName;

    // 등급코드
    @Column(name = "rank_code", length = 10)
    private String rankCode;

    // 지역코드
    @Column(name = "region_code", length = 10)
    private String regionCode;

    // 지역명
    @Column(name = "region_name", length = 50)
    private String regionName;

    // 시장코드
    @Column(name = "market_code", nullable = false, length = 10)
    private String marketCode;

    // 시장명
    @Column(name = "market_name", length = 100)
    private String marketName;

    // 시장 가격
    @Column(name = "price")
    private Integer price;

    // 가격 단위
    @Column(name = "unit", length = 20)
    private String unit;

    // 가격 조사 날짜
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    // 거래 유형 (소매/도매)
    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    private MarketType marketType;

    public enum MarketType {
        RETAIL,     // 소매
        WHOLESALE   // 도매
    }

    @Builder
    public MarketPrice(
            String itemCode, String itemName,
            String kindCode, String kindName,
            String rankCode,
            String regionCode, String regionName,
            String marketCode, String marketName,
            Integer price, String unit,
            LocalDate priceDate, MarketType marketType
    ) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.kindCode = kindCode;
        this.kindName = kindName;
        this.rankCode = rankCode;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.marketCode = marketCode;
        this.marketName = marketName;
        this.price = price;
        this.unit = unit;
        this.priceDate = priceDate;
        this.marketType = marketType;
    }
}
