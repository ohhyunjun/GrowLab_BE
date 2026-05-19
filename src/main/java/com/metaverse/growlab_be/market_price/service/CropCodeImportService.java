package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CropCodeImportService {

    private final CropCodeRepository cropCodeRepository;

    public void importCropCodes() {

        try {

            ClassPathResource resource =
                    new ClassPathResource("excel/kamis_crop_code.xlsx");

            InputStream inputStream = resource.getInputStream();

            Workbook workbook = WorkbookFactory.create(inputStream);

            Sheet sheet = workbook.getSheetAt(0);

            log.info("시트 이름 = {}", sheet.getSheetName());

        } catch (Exception e) {

            log.error("엑셀 파일 읽기 실패", e);
        }
    }
}
