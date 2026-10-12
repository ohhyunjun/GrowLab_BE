package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import com.metaverse.growlab_be.market_price.repository.MarketPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketPriceServiceTest {

    private MarketPriceRepository marketPriceRepository;
    private MarketPriceService marketPriceService;

    @BeforeEach
    void setUp() {
        marketPriceRepository = mock(MarketPriceRepository.class);
        marketPriceService = new MarketPriceService(
                marketPriceRepository, mock(CropCodeRepository.class), mock(WebClient.class));
    }

    @Test
    void returnsRowsForLatestSevenDistinctTradingDatesForRetail() {
        assertSevenTradingDates(MarketPrice.MarketType.RETAIL);
    }

    @Test
    void returnsRowsForLatestSevenDistinctTradingDatesForWholesale() {
        assertSevenTradingDates(MarketPrice.MarketType.WHOLESALE);
    }

    @Test
    void returnsEmptyWhenMarketTypeHasNoPublishedData() {
        when(marketPriceRepository.findLatestDistinctPriceDates(
                eq("422"), eq("01"), eq(MarketPrice.MarketType.WHOLESALE), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(marketPriceService.getWeeklyPrices(
                "422", "01", MarketPrice.MarketType.WHOLESALE)).isEmpty();

        verify(marketPriceRepository, never())
                .findByItemCodeAndKindCodeAndMarketTypeAndPriceDateInOrderByPriceDateAsc(
                        any(), any(), any(), any());
    }

    private void assertSevenTradingDates(MarketPrice.MarketType marketType) {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31));
        List<MarketPrice> rows = List.of(priceAt(dates.get(6), marketType), priceAt(dates.get(0), marketType));

        when(marketPriceRepository.findLatestDistinctPriceDates(
                eq("214"), eq("02"), eq(marketType), any(Pageable.class))).thenReturn(dates);
        when(marketPriceRepository.findByItemCodeAndKindCodeAndMarketTypeAndPriceDateInOrderByPriceDateAsc(
                "214", "02", marketType, dates)).thenReturn(rows);

        assertThat(marketPriceService.getWeeklyPrices("214", "02", marketType))
                .containsExactlyElementsOf(rows);
        verify(marketPriceRepository).findLatestDistinctPriceDates(
                eq("214"), eq("02"), eq(marketType),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 7));
    }

    private MarketPrice priceAt(LocalDate date, MarketPrice.MarketType marketType) {
        return MarketPrice.builder()
                .itemCode("214")
                .itemName("상추")
                .kindCode("02")
                .marketCode("1101")
                .price(10_000)
                .priceDate(date)
                .marketType(marketType)
                .build();
    }
}
