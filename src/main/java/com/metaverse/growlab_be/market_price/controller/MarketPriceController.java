package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class MarketPriceController {

    private final MarketPriceService marketPriceService;


    // 최신 가격 조회
    // 예:
    // /api/prices/latest?itemCode=422&kindCode=00
    @GetMapping("/latest")
    public ResponseEntity<MarketPriceResponseDto> getLatestPrice(
            @RequestParam String itemCode,
            @RequestParam String kindCode
    ) {

        MarketPrice marketPrice =
                marketPriceService.getLatestPrice(itemCode, kindCode);

        return ResponseEntity.ok(
                MarketPriceResponseDto.of(marketPrice)
        );
    }

    // 최근 7일 가격 조회
    // 예:
    // /api/prices/weekly?itemCode=422&kindCode=00
    @GetMapping("/weekly")
    public ResponseEntity<MarketPriceResponseDto> getWeeklyPrices(
            @RequestParam String itemCode,
            @RequestParam String kindCode
    ) {

        return ResponseEntity.ok(
                MarketPriceResponseDto.ofHistory(
                        itemCode,
                        kindCode,
                        marketPriceService.getWeeklyPrices(itemCode, kindCode)
                )
        );
    }

    // 수동 가격 수집 실행(어제 하루치 수집)
    // 예:
    // POST /api/prices/fetch
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchMarketPrices() {

        marketPriceService.fetchAndSaveMarketPrice();

        return ResponseEntity.ok("KAMIS 가격 데이터 수집 완료");
    }

    // ✅ 추가 - 날짜 범위 수집 (초기 수집용)
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