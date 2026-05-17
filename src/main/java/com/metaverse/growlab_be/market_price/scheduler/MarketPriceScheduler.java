package com.metaverse.growlab_be.market_price.scheduler;

import com.metaverse.growlab_be.market_price.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceScheduler {

    private final MarketPriceService marketPriceService;

    // 매일 오전 6시에 자동으로 실행되어 외부 API 데이터를 동기화하는 스케줄러
    // cron 표현식: 초 분 시 일 월 요일
    @Scheduled(cron = "0 0 6 * * *")
    public void collectionMarketPriceJob() {
        log.info("=== 농산물 도소매 가격 데이터 동기화 스케줄러 시작 ===");
        try {
            marketPriceService.fetchAndSaveMarketPrice();
            log.info("=== 농산물 도소매 가격 데이터 동기화 스케줄러 완료 ===");
        } catch (Exception e) {
            log.error("농산물 가격 데이터 수집 중 예외 발생: ", e);
        }
    }
}
