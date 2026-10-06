package com.metaverse.growlab_be.market_price.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPriceSchedulerTest {

    @Test
    void runsAtSixAmInSeoulTime() throws NoSuchMethodException {
        Scheduled scheduled = MarketPriceScheduler.class
                .getMethod("collectionMarketPriceJob")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 6 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }
}
