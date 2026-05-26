package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import lombok.Getter;

@Getter
public class CropCodeResponseDto {

    private final String itemCategoryCode;  // 부류코드
    private final String itemCategoryName;  // 부류명
    private final String itemCode;          // 품목코드
    private final String itemName;          // 품목명
    private final String kindCode;          // 품종코드
    private final String kindName;          // 품종명
    private final String rankCode;          // 도매 등급코드
    private final String wholesaleUnit;     // 도매 출하단위
    private final String retailUnit;        // 소매 출하단위

    public CropCodeResponseDto(CropCode cropCode) {
        this.itemCategoryCode = cropCode.getItemCategoryCode();
        this.itemCategoryName = cropCode.getItemCategoryName();
        this.itemCode = cropCode.getItemCode();
        this.itemName = cropCode.getItemName();
        this.kindCode = cropCode.getKindCode();
        this.kindName = cropCode.getKindName();
        this.rankCode = cropCode.getRankCode();
        this.wholesaleUnit = combineUnit(
                cropCode.getWholesaleUnitSize(),
                cropCode.getWholesaleUnit()
        );
        this.retailUnit = combineUnit(
                cropCode.getRetailUnitSize(),
                cropCode.getRetailUnit()
        );
    }
    private String combineUnit(String size, String unit) {
        if (unit == null || unit.isBlank()) return null;
        if (size == null || size.isBlank()) return unit;
        return size + unit;
    }
}
