package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MarketPriceResponseDto {

    private final String itemName;
    private final Integer wholesalePrice;
    private final Integer retailPrice;
    private final String priceUnit;
    private final LocalDate priceDate;

    public MarketPriceResponseDto(MarketPrice marketPrice) {
        this.itemName = marketPrice.getItemName();
        this.wholesalePrice = marketPrice.getWholesalePrice();
        this.retailPrice = marketPrice.getRetailPrice();
        this.priceUnit = marketPrice.getPriceUnit();
        this.priceDate = marketPrice.getPriceDate();
    }
}
