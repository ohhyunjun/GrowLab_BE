package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    // 1. 특정 품목의 가장 최신 가격 데이터 1개 가져오기
    Optional<MarketPrice> findFirstByItemNameOrderByPriceDateDesc(String itemName);

    // 2. 특정 품목의 최근 N일간 가격 내역 가져오기
    // 가격 조사 일자가 특정 시작일자(startDate) 이후인 데이터들을 날짜 오름차순으로 정렬해 가져옵니다.
    List<MarketPrice> findByItemNameAndPriceDateGreaterThanEqualOrderByPriceDateAsc(String itemName, LocalDate startDate);

}
