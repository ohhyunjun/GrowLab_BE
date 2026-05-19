package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CropCodeRepository extends JpaRepository<CropCode, Long> {

    List<CropCode> findByKindNameContaining(String kindName);
}
