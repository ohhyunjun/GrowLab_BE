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
    // (API 스펙의 item_name 혹은 item_code와 매핑)
    @Column(name = "item_name", nullable = false, length = 50)
    private String itemName;

    // 도매 가격
    // (API 스펙의 dpr1 또는 wholesale_price와 매핑)
    @Column(name = "wholesale_price")
    private Integer wholesalePrice;

    // 소매 가격
    // (API 스펙의 dpr2 또는 retail_price와 매핑)
    @Column(name = "retail_price")
    private Integer retailPrice;

    // 가격 단위
    // (API 스펙의 unit 혹은 kind_name과 매핑)
    @Column(name = "price_unit", length = 20)
    private String priceUnit;

    // 가격 조사 기준 날짜
    // API 스펙의 yyyy-MM-dd 형태의 날짜 데이터와 매핑)
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Builder
    public MarketPrice(String itemName, Integer wholesalePrice, Integer retailPrice, String priceUnit, LocalDate priceDate) {
        this.itemName = itemName;
        this.wholesalePrice = wholesalePrice;
        this.retailPrice = retailPrice;
        this.priceUnit = priceUnit;
        this.priceDate = priceDate;
    }
}
