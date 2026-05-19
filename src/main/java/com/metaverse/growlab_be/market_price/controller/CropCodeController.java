package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.dto.CropCodeResponseDto;
import com.metaverse.growlab_be.market_price.service.CropCodeImportService;
import com.metaverse.growlab_be.market_price.service.CropCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropCodeController {

    private final CropCodeImportService cropCodeImportService;
    private final CropCodeService cropCodeService;

    // 엑셀 파일에서 작물 코드 데이터를 읽어와 DB에 저장하는 API
    // http://localhost:8080/api/crops/import
    @GetMapping("/import")
    public ResponseEntity<String> importCropCodes() {

        cropCodeImportService.importCropCodes();

        return ResponseEntity.ok("엑셀 파일 읽기 성공");
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
