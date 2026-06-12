package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    /**
     * 최신 가격 조회
     * 예: /api/prices/latest?itemCode=422&kindCode=00&marketType=RETAIL
     *     /api/prices/latest?itemCode=422&kindCode=00&marketType=WHOLESALE
     */
    @GetMapping("/latest")
    public ResponseEntity<MarketPriceResponseDto> getLatestPrice(
            @RequestParam String itemCode,
            @RequestParam String kindCode,
            @RequestParam(defaultValue = "RETAIL") MarketPrice.MarketType marketType
    ) {
        return ResponseEntity.ok(
                MarketPriceResponseDto.of(
                        marketPriceService.getLatestPrice(itemCode, kindCode, marketType)
                )
        );
    }

    /**
     * 최근 7일 가격 조회
     * 예: /api/prices/weekly?itemCode=422&kindCode=00&marketType=RETAIL
     *     /api/prices/weekly?itemCode=422&kindCode=00&marketType=WHOLESALE
     */
    @GetMapping("/weekly")
    public ResponseEntity<MarketPriceResponseDto> getWeeklyPrices(
            @RequestParam String itemCode,
            @RequestParam String kindCode,
            @RequestParam(defaultValue = "RETAIL") MarketPrice.MarketType marketType
    ) {
        return ResponseEntity.ok(
                MarketPriceResponseDto.ofHistory(
                        itemCode,
                        kindCode,
                        marketPriceService.getWeeklyPrices(itemCode, kindCode, marketType)
                )
        );
    }

    /**
     * 수동 가격 수집 (어제 하루치)
     * POST /api/prices/fetch
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchMarketPrices() {
        marketPriceService.fetchAndSaveMarketPrice();
        return ResponseEntity.ok("KAMIS 가격 데이터 수집 완료");
    }

    /**
     * 날짜 범위 수집 (초기 수집용)
     * POST /api/prices/fetch/range?startDate=2025-06-01&endDate=2025-06-12
     */
    @PostMapping("/fetch/range")
    public ResponseEntity<String> fetchMarketPricesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        marketPriceService.fetchAndSaveByDateRange(startDate, endDate);
        return ResponseEntity.ok(
                "KAMIS 가격 데이터 수집 완료 (" + startDate + " ~ " + endDate + ")"
        );
    }
}