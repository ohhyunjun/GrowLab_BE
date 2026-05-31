package com.metaverse.growlab_be.market_price.dto;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MarketPriceResponseDto {

    // 품목 정보
    private String itemCode;
    private String itemName;
    private String kindCode;
    private String kindName;

    // 가격 정보
    private Integer currentPrice;
    private Double averagePrice;
    private String unit;

    // 변동 정보
    private Double changeRate;
    private ChangeStatus changeStatus;

    // 지역/시장 정보
    private String regionCode;
    private String regionName;
    private String marketName;
    private String marketType;

    // 가격 히스토리
    private List<PriceHistoryDto> priceHistory;

    public enum ChangeStatus {
        SHARP_RISE,  // +5% 이상
        RISE,        // +1~5%
        STABLE,      // -1~+1%
        FALL,        // -1~-5%
        SHARP_FALL   // -5% 이하
    }

    @Getter
    @Builder
    public static class PriceHistoryDto {
        private LocalDate date;
        private Integer price;
    }

    // ─── 팩토리 메서드 ──────────────────────────────────────

    // 최신 가격 1건 응답
    public static MarketPriceResponseDto of(MarketPrice mp) {
        return MarketPriceResponseDto.builder()
                .itemCode(mp.getItemCode())
                .itemName(mp.getItemName())
                .kindCode(mp.getKindCode())
                .kindName(mp.getKindName())
                .currentPrice(mp.getPrice())
                .unit(mp.getUnit())
                .regionCode(mp.getRegionCode())
                .regionName(mp.getRegionName())
                .marketName(mp.getMarketName())
                .marketType(mp.getMarketType().name())
                .build();
    }

    // 7일 히스토리 응답
    public static MarketPriceResponseDto ofHistory(
            String itemCode, String kindCode, List<MarketPrice> prices) {

        if (prices == null || prices.isEmpty()) {
            return MarketPriceResponseDto.builder()
                    .itemCode(itemCode)
                    .kindCode(kindCode)
                    .build();
        }

        // 현재 가격 (가장 최근)
        MarketPrice latest = prices.get(prices.size() - 1);
        Integer currentPrice = latest.getPrice();

        // 평균 가격
        double averagePrice = prices.stream()
                .mapToInt(MarketPrice::getPrice)
                .average()
                .orElse(0);

        // 전일 대비 변동률
        Double changeRate = null;
        ChangeStatus changeStatus = ChangeStatus.STABLE;

        if (prices.size() >= 2) {
            Integer yesterdayPrice = prices.get(prices.size() - 2).getPrice();
            if (yesterdayPrice != null && yesterdayPrice != 0) {
                changeRate = (double) (currentPrice - yesterdayPrice) / yesterdayPrice * 100;
                changeRate = Math.round(changeRate * 10.0) / 10.0;
                changeStatus = resolveChangeStatus(changeRate);
            }
        }

        // 히스토리
        List<PriceHistoryDto> history = prices.stream()
                .map(mp -> PriceHistoryDto.builder()
                        .date(mp.getPriceDate())
                        .price(mp.getPrice())
                        .build())
                .collect(Collectors.toList());

        return MarketPriceResponseDto.builder()
                .itemCode(latest.getItemCode())
                .itemName(latest.getItemName())
                .kindCode(latest.getKindCode())
                .kindName(latest.getKindName())
                .currentPrice(currentPrice)
                .averagePrice(Math.round(averagePrice * 10.0) / 10.0)
                .unit(latest.getUnit())
                .changeRate(changeRate)
                .changeStatus(changeStatus)
                .regionCode(latest.getRegionCode())
                .regionName(latest.getRegionName())
                .marketName(latest.getMarketName())
                .marketType(latest.getMarketType().name())
                .priceHistory(history)
                .build();
    }

    private static ChangeStatus resolveChangeStatus(Double changeRate) {
        if (changeRate >= 5.0) return ChangeStatus.SHARP_RISE;
        if (changeRate >= 1.0) return ChangeStatus.RISE;
        if (changeRate > -1.0) return ChangeStatus.STABLE;
        if (changeRate > -5.0) return ChangeStatus.FALL;
        return ChangeStatus.SHARP_FALL;
    }
}