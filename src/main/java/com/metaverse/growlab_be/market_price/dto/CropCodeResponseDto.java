package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import lombok.Getter;

@Getter
public class CropCodeResponseDto {

    private final String itemCode;
    private final String kindCode;
    private final String kindName;
    private final String unit;

    public CropCodeResponseDto(CropCode cropCode) {

        this.itemCode = cropCode.getItemCode();
        this.kindCode = cropCode.getKindCode();
        this.kindName = cropCode.getKindName();
        this.unit = cropCode.getUnit();
    }
}
