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
                        name = "uk_item_date",
                        columnNames = {"item_name", "price_date"} // 같은 날짜에 동일 품목은 중복 저장 방지
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

    @Builder
    public MarketPrice(
            String itemName,
            Integer price,
            String unit,
            String marketName,
            String countyName,
            LocalDate priceDate
    ) {
        this.itemName = itemName;
        this.price = price;
        this.unit = unit;
        this.marketName = marketName;
        this.countyName = countyName;
        this.priceDate = priceDate;
    }
}
