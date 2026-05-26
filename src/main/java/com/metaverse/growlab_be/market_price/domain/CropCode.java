package com.metaverse.growlab_be.market_price.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crop_code",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_kind",
                columnNames = {"item_code", "kind_code"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CropCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_category_code", length = 30)
    private String itemCategoryCode;        // 부류코드

    @Column(name = "item_category_name", length = 20)
    private String itemCategoryName;        // 부류명

    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;                // 품목코드

    @Column(name = "item_name", nullable = false, length = 50)
    private String itemName;                // 품목명

    @Column(name = "kind_code", nullable = false, length = 30)
    private String kindCode;                // 품종코드

    @Column(name = "kind_name", length = 50)
    private String kindName;                // 품종명

    @Column(name = "rank_code", length = 30)
    private String rankCode;                // 도매 등급코드

    @Column(name = "retail_rank_code", length = 30)
    private String retailRankCode;          // 소매 등급코드

    @Column(name = "eco_rank_code", length = 30)
    private String ecoRankCode;             // 친환경 등급코드

    @Column(name = "wholesale_unit", length = 20)
    private String wholesaleUnit;           // 도매 출하단위

    @Column(name = "wholesale_unit_size", length = 30)
    private String wholesaleUnitSize;       // 도매 출하단위 크기

    @Column(name = "retail_unit", length = 20)
    private String retailUnit;              // 소매 출하단위

    @Column(name = "retail_unit_size", length = 30)
    private String retailUnitSize;          // 소매 출하단위 크기

    @Column(name = "eco_unit", length = 20)
    private String ecoUnit;                 // 친환경 출하단위

    @Column(name = "eco_unit_size", length = 30)
    private String ecoUnitSize;             // 친환경 출하단위 크기


    @Builder
    public CropCode(
            String itemCategoryCode, String itemCategoryName,
            String itemCode, String itemName,
            String kindCode, String kindName,
            String rankCode,
            String retailRankCode, String ecoRankCode,
            String wholesaleUnit, String wholesaleUnitSize,
            String retailUnit, String retailUnitSize,
            String ecoUnit, String ecoUnitSize
    ) {
        this.itemCategoryCode = itemCategoryCode;
        this.itemCategoryName = itemCategoryName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.kindCode = kindCode;
        this.kindName = kindName;
        this.rankCode = rankCode;
        this.retailRankCode = retailRankCode;
        this.ecoRankCode = ecoRankCode;
        this.wholesaleUnit = wholesaleUnit;
        this.wholesaleUnitSize = wholesaleUnitSize;
        this.retailUnit = retailUnit;
        this.retailUnitSize = retailUnitSize;
        this.ecoUnit = ecoUnit;
        this.ecoUnitSize = ecoUnitSize;
    }
}
