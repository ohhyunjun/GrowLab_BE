package com.metaverse.growlab_be.market_price.repository;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CropCodeRepository extends JpaRepository<CropCode, Long> {

    // 작물 이름으로 품목코드, 품종코드, 단위를 조회하는 메서드
    List<CropCode> findByKindNameContaining(String kindName);

    // itemCode + kindCode 기준 중복 저장 방지
    boolean existsByItemCodeAndKindCode(
            String itemCode,
            String kindCode
    );
}
