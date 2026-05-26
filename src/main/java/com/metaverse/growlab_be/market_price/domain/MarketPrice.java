package com.metaverse.growlab_be.market_price.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "market_price",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_date_market_type",
                        columnNames = {"item_name", "price_date", "market_type"}
                )
        }
)

public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 품목명
    @Column(name = "item_name", nullable = false, length = 50)
    private String itemName;

    // 시장 가격
    @Column(name = "price")
    private Integer price;

    // 가격 단위
    @Column(name = "unit", length = 20)
    private String unit;

    // 시장명
    @Column(name = "market_name", length = 100)
    private String marketName;

    // 지역명
    @Column(name = "county_name", length = 50)
    private String countyName;

    // 가격 조사 날짜
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    // 거래 유형 (소매/도매)
    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    private MarketType marketType;

    // CropCode과의 연관 관계 설정(단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_code_id")
    private CropCode cropCode;

    public enum MarketType {
        RETAIL,     // 소매
        WHOLESALE   // 도매
    }

    @Builder
    public MarketPrice(
            String itemName,
            Integer price,
            String unit,
            String marketName,
            String countyName,
            LocalDate priceDate,
            MarketType marketType,
            CropCode cropCode
    ) {
        this.itemName = itemName;
        this.price = price;
        this.unit = unit;
        this.marketName = marketName;
        this.countyName = countyName;
        this.priceDate = priceDate;
        this.marketType = marketType;
        this.cropCode = cropCode;
    }
}
