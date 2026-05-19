package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropCodeRepository extends JpaRepository<CropCode, Long> {
}
