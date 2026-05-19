package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
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

            Sheet sheet = workbook.getSheet("코드통합(부류+품목+품종코드)");

            log.info("시트 이름 = {}", sheet.getSheetName());

            log.info("총 행 개수 = {}", sheet.getPhysicalNumberOfRows());

            for (int i = 2; i < sheet.getPhysicalNumberOfRows(); i++) {

                Row row = sheet.getRow(i);
                String itemCode = row.getCell(3).toString().trim();
                String itemName = row.getCell(4).toString().trim();
                String kindCode = row.getCell(5).toString().trim();
                String kindName = row.getCell(6).toString().trim();
                String unit = row.getCell(13).toString().trim();

                // 중복 저장 방지
                boolean exists = cropCodeRepository
                        .existsByItemCodeAndKindCode(itemCode, kindCode);

                if (exists) {
                    log.info(
                            "이미 저장된 데이터 스킵 - itemCode={}, kindCode={}",
                            itemCode,
                            kindCode
                    );
                    continue;
                }

                CropCode cropCode = CropCode.builder()
                        .itemName(itemName)
                        .itemCode(itemCode)
                        .kindCode(kindCode)
                        .kindName(kindName)
                        .unit(unit)
                        .build();

                cropCodeRepository.save(cropCode);

                log.info(
                        "품목명={}, 품목코드={}, 품종명={}, 품종코드={}, 단위={}",
                        itemName, itemCode, kindName, kindCode, unit
                );
            }

            log.info("CropCode 엑셀 데이터 저장 완료");

        } catch (Exception e) {

            log.error("엑셀 파일 읽기 실패", e);
        }
    }
}
