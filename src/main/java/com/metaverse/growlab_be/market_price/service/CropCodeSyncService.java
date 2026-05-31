package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import com.metaverse.growlab_be.market_price.dto.KamisItemDto;
import com.metaverse.growlab_be.market_price.dto.KamisResponseDto;
import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CropCodeSyncService {

    private final CropCodeRepository cropCodeRepository;
    private final WebClient kamisWebClient;

    @Value("${kamis.api.key}")
    private String certKey;

    @Value("${kamis.api.id}")
    private String certId;

    @Transactional
    public void syncCropCodes() {
        log.info("[CropCodeSync] 품목코드 수집 시작");

        KamisResponseDto response = fetchFromKamis();

        if (response == null || response.getInfo() == null) {  // ← getData().getItem() → getInfo()
            log.warn("[CropCodeSync] KAMIS 응답 없음 또는 데이터 없음");
            return;
        }

        if (!response.isSuccess()) {  // ← error_code 체크 추가
            log.warn("[CropCodeSync] KAMIS 에러 - error_code: {}", response.getErrorCode());
            return;
        }

        int savedCount = 0;
        int skipCount = 0;

        for (KamisItemDto item : response.getInfo()) {  // ← getData().getItem() → getInfo()
            try {
                if (!isValidItem(item)) {
                    skipCount++;
                    continue;
                }

                boolean exists = cropCodeRepository
                        .existsByItemCodeAndKindCode(
                                safe(item.getItemcode()),
                                safe(item.getKindcode())
                        );

                if (exists) {
                    skipCount++;
                    continue;
                }

                CropCode cropCode = toEntity(item);
                if (cropCode != null) {
                    cropCodeRepository.save(cropCode);
                    savedCount++;
                }

            } catch (Exception e) {
                log.warn("[CropCodeSync] 항목 저장 실패 - itemCode: {}, error: {}",
                        item.getItemcode(), e.getMessage());
                skipCount++;
            }
        }

        log.info("[CropCodeSync] 완료 - 신규 저장: {}건, 스킵: {}건",
                savedCount, skipCount);
    }

    private boolean isValidItem(KamisItemDto item) {
        if (item.getItemcode() == null || item.getItemcode().isBlank()) {
            log.warn("[CropCodeSync] itemcode 없음 - 스킵");
            return false;
        }
        if (item.getItemname() == null || item.getItemname().isBlank()) {
            log.warn("[CropCodeSync] itemname 없음 - itemCode: {}", item.getItemcode());
            return false;
        }
        if (item.getKindcode() == null || item.getKindcode().isBlank()) {
            log.warn("[CropCodeSync] kindcode 없음 - itemCode: {}", item.getItemcode());
            return false;
        }
        return true;
    }

    private KamisResponseDto fetchFromKamis() {
        try {
            return kamisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/service/price/xml.do")
                            .queryParam("action", "productInfo")
                            .queryParam("p_cert_key", certKey)
                            .queryParam("p_cert_id", certId)
                            .queryParam("p_returntype", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(KamisResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.error("[CropCodeSync] KAMIS API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private CropCode toEntity(KamisItemDto item) {
        try {
            return CropCode.builder()
                    .itemCategoryCode(safeOrNull(item.getItemcategorycode()))
                    .itemCategoryName(safeOrNull(item.getItemcategoryname()))
                    .itemCode(safe(item.getItemcode()))
                    .itemName(safe(item.getItemname()))
                    .kindCode(safe(item.getKindcode()))
                    .kindName(safeOrNull(item.getKindname()))
                    // 도매 등급
                    .rankCode(safeOrNull(item.getWhole_productrankcode()))
                    // 소매 등급
                    .retailRankCode(safeOrNull(item.getRetail_productrankcode()))
                    // 친환경 등급
                    .ecoRankCode(safeOrNull(item.getNew_natreu_productrankcode()))
                    // 도매 단위
                    .wholesaleUnit(safeOrNull(item.getWholesale_unit()))
                    .wholesaleUnitSize(safeOrNull(item.getWholesale_unitsize()))
                    // 소매 단위
                    .retailUnit(safeOrNull(item.getRetail_unit()))
                    .retailUnitSize(safeOrNull(item.getRetail_unitsize()))
                    // 친환경 단위
                    .ecoUnit(safeOrNull(item.getEco_unit()))
                    .ecoUnitSize(safeOrNull(item.getEco_unitsize()))
                    .build();
        } catch (Exception e) {
            log.warn("[CropCodeSync] 항목 변환 실패 - itemCode: {}, error: {}",
                    item.getItemcode(), e.getMessage());
            return null;
        }
    }

    // nullable = false 컬럼용 (null → "")
    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    // nullable 허용 컬럼용 (null → null)
    private String safeOrNull(String value) {
        return value != null ? value.trim() : null;
    }
}
