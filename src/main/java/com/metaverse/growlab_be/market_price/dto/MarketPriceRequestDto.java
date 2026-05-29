package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
public class MarketPriceRequestDto {

    // KAMIS API 날짜 포맷
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 조회 시작일
    private final LocalDate startDate;

    // 조회 종료일
    private final LocalDate endDate;

    // 부류코드 (100=식량작물, 200=채소류, 300=특용작물, 400=과일류, 600=수산물)
    private final String itemCategoryCode;

    // 품목코드 (예: 422=방울토마토, 211=배추)
    private final String itemCode;

    // 품종코드 (예: 00, 01)
    private final String kindCode;

    // 등급코드 (예: 04=상품, 05=중품)
    private final String rankCode;

    // 지역코드 (예: 1101=서울) default: 1101
    private final String regionCode;

    // kg 단위 환산 여부 default: Y
    private final String convertKgYn;

    // 거래유형 (RETAIL=소매, WHOLESALE=도매)
    private final MarketPrice.MarketType marketType;

    @Builder
    public MarketPriceRequestDto(
            LocalDate startDate, LocalDate endDate,
            String itemCategoryCode,
            String itemCode, String kindCode,
            String rankCode, String regionCode,
            String convertKgYn,
            MarketPrice.MarketType marketType
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.itemCategoryCode = itemCategoryCode;
        this.itemCode = itemCode;
        this.kindCode = kindCode;
        this.rankCode = rankCode;
        this.regionCode = regionCode != null ? regionCode : "1101";
        this.convertKgYn = convertKgYn != null ? convertKgYn : "Y";
        this.marketType = marketType;
    }

    // KAMIS API action 파라미터 반환
    public String getAction() {
        return marketType == MarketPrice.MarketType.RETAIL
                ? "periodRetailProductList"
                : "periodWholesaleProductList";
    }

    // KAMIS API용 날짜 포맷 변환 (yyyy-MM-dd)
    public String getFormattedStartDate() {
        return startDate.format(DATE_FORMATTER);
    }

    public String getFormattedEndDate() {
        return endDate.format(DATE_FORMATTER);
    }

}
