package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;


    // 1. 특정 품목의 가장 최신 도소매 가격 조회
    public MarketPriceResponseDto getLatestPrice(String itemName) {
        MarketPrice marketPrice = marketPriceRepository.findFirstByItemNameOrderByPriceDateDesc(itemName)
                .orElseThrow(() -> new IllegalArgumentException("해당 품목의 가격 데이터가 존재하지 않습니다: " + itemName));

        return new MarketPriceResponseDto(marketPrice);
    }


    // 2. 특정 품목의 최근 일주일(7일)간 가격 추이 조회
    public List<MarketPriceResponseDto> getWeeklyPriceMovement(String itemName) {
        // 오늘 날짜 기준으로 6일 전부터 오늘까지 총 7일간의 데이터를 타겟으로 잡음
        LocalDate startDate = LocalDate.now().minusDays(6);

        List<MarketPrice> prices = marketPriceRepository
                .findByItemNameAndPriceDateGreaterThanEqualOrderByPriceDateAsc(itemName, startDate);

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환 (.stream() 활용)
        return prices.stream()
                .map(MarketPriceResponseDto::new)
                .collect(Collectors.toList());
    }

     // 3. (추후 구현) 외부 오픈 API(KAMIS/공공데이터) 호출 및 DB 저장 로직이 들어갈 자리
     // 외부에서 JSON 데이터를 땡겨와 정규화(단위 맞추기) 후 저장하는 메서드가 될 것입니다.
    @Transactional
    public void fetchAndSaveMarketPrice() {
        // TODO: Open API 연동 키 발급 후 구현 예정
        log.info("외부 농산물 가격 API 호출 및 DB 동기화 스케줄러 작동 예정");
    }
}