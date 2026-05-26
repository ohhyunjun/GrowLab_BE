package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.dto.CropCodeResponseDto;
import com.metaverse.growlab_be.market_price.service.CropCodeSyncService;
import com.metaverse.growlab_be.market_price.service.CropCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropCodeController {

    private final CropCodeSyncService cropCodeSyncService;
    private final CropCodeService cropCodeService;

    // KAMIS API에서 품목코드를 수집하여 DB에 저장하는 API
    // http://localhost:8080/api/crops/import
    @PostMapping ("/sync")
    public ResponseEntity<String> syncCropCodes() {
        cropCodeSyncService.syncCropCodes();
        return ResponseEntity.ok("KAMIS API 품목코드 수집 완료");
    }

    // 작물 이름으로 품목코드, 품종코드, 단위를 조회하는 API
    // http://localhost:8080/api/crops/search?name=방울토마토
    @GetMapping("/search")
    public ResponseEntity<List<CropCodeResponseDto>> searchCrop(
            @RequestParam String name
    ) {

        return ResponseEntity.ok(
                cropCodeService.searchCrop(name)
        );
    }
}
