package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MarketPriceResponseDto {

    private final String itemName;
    private final Integer price;
    private final String unit;
    private final String marketName;
    private final String countyName;
    private final LocalDate priceDate;


    public MarketPriceResponseDto(MarketPrice marketPrice) {
        this.itemName = marketPrice.getItemName();
        this.price = marketPrice.getPrice();
        this.unit = marketPrice.getUnit();
        this.marketName = marketPrice.getMarketName();
        this.countyName = marketPrice.getCountyName();
        this.priceDate = marketPrice.getPriceDate();
    }
}
