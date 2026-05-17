package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class MarketPriceController {

    private final MarketPriceService marketPriceService;


    // 1. 특정 품목의 가장 최신 도소매 가격 조회
    @GetMapping("/latest")
    public ResponseEntity<MarketPriceResponseDto> getLatestPrice(@RequestParam("itemName") String itemName) {
        MarketPriceResponseDto response = marketPriceService.getLatestPrice(itemName);
        return ResponseEntity.ok(response);
    }

    // 2. 특정 품목의 최근 일주일(7일)간 가격 추이 조회
    @GetMapping("/weekly")
    public ResponseEntity<List<MarketPriceResponseDto>> getWeeklyPrices(@RequestParam("itemName") String itemName) {
        List<MarketPriceResponseDto> responses = marketPriceService.getWeeklyPriceMovement(itemName);
        return ResponseEntity.ok(responses);
    }
}