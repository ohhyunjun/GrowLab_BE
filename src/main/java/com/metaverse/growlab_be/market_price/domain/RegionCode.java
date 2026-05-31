package com.metaverse.growlab_be.market_price.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region_code",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_region_market",
                columnNames = {"region_code", "market_code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지역코드 (1101, 2100 ...)
    @Column(name = "region_code", nullable = false, length = 10)
    private String regionCode;

    // 지역명 (서울, 부산 ...)
    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    // 시장코드 (1101, 2100 ...)
    @Column(name = "market_code", nullable = false, length = 10)
    private String marketCode;

    // 시장명 (가락도매, 각화도매 ...)
    @Column(name = "market_name", nullable = false, length = 100)
    private String marketName;

    // 거래유형 (RETAIL/WHOLESALE/ECO/ONLINE)
    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    private MarketType marketType;

    // 산물분류코드 (01=소매, 02=중도매, 07=친환경, 09=온라인)
    @Column(name = "product_cls_code", nullable = false, length = 5)
    private String productClsCode;

    @Builder
    public RegionCode(String regionCode, String regionName, String marketCode,
                      String marketName, MarketType marketType, String productClsCode) {
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.marketCode = marketCode;
        this.marketName = marketName;
        this.marketType = marketType;
        this.productClsCode = productClsCode;
    }

    public enum MarketType {
        RETAIL,      // 01 = 소매
        WHOLESALE,   // 02 = 중도매(도매)
        ECO,         // 07 = 친환경
        ONLINE       // 09 = 온라인
    }
}
