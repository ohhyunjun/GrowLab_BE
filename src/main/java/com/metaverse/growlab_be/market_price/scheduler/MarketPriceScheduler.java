package com.metaverse.growlab_be.market_price.scheduler;

import com.metaverse.growlab_be.market_price.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceScheduler {

    private final MarketPriceService marketPriceService;
}
