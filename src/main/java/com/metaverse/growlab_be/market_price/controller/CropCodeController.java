package com.metaverse.growlab_be.market_price.controller;

import com.metaverse.growlab_be.market_price.service.CropCodeImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropCodeController {

    private final CropCodeImportService cropCodeImportService;

    // http://localhost:8080/api/crops/import
    @GetMapping("/import")
    public ResponseEntity<String> importCropCodes() {

        cropCodeImportService.importCropCodes();

        return ResponseEntity.ok("엑셀 파일 읽기 성공");
    }
}
