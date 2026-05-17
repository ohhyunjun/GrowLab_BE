package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {
}
