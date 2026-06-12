package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.KamisItemDto;
import com.metaverse.growlab_be.market_price.dto.KamisPriceResponseDto;
import com.metaverse.growlab_be.market_price.dto.MarketPriceRequestDto;
import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import com.metaverse.growlab_be.market_price.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;
    private final CropCodeRepository cropCodeRepository;
    private final WebClient kamisWebClient;

    @Value("${kamis.api.key}")
    private String certKey;

    @Value("${kamis.api.id}")
    private String certId;

    // ─── 조회 ────────────────────────────────────────────────

    // 최신 소매가 1건
    public MarketPrice getLatestPrice(String itemCode, String kindCode) {
        return marketPriceRepository
                .findFirstByItemCodeAndKindCodeAndMarketTypeOrderByPriceDateDesc(
                        itemCode, kindCode, MarketPrice.MarketType.RETAIL)
                .orElseThrow(() -> new IllegalArgumentException(
                        "가격 데이터가 없습니다. itemCode=" + itemCode + ", kindCode=" + kindCode));
    }

    // 최근 7일 소매가
    public List<MarketPrice> getWeeklyPrices(String itemCode, String kindCode) {
        LocalDate startDate = LocalDate.now().minusDays(6);
        return marketPriceRepository
                .findByItemCodeAndKindCodeAndMarketTypeAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                        itemCode, kindCode, MarketPrice.MarketType.RETAIL, startDate);
    }

    // ─── 수집 ────────────────────────────────────────────────

    @Transactional
    public void fetchAndSaveMarketPrice() {
        log.info("[MarketPrice] 가격 수집 시작");

        List<CropCode> cropCodes = cropCodeRepository.findAll();

        if (cropCodes.isEmpty()) {
            log.warn("[MarketPrice] CropCode 없음. /api/crops/sync 먼저 실행 필요");
            return;
        }

        int totalSaved = 0;

        for (CropCode cropCode : cropCodes) {
            try {
                int retailSaved = fetchAndSave(cropCode, MarketPrice.MarketType.RETAIL);
                int wholesaleSaved = fetchAndSave(cropCode, MarketPrice.MarketType.WHOLESALE);
                totalSaved += retailSaved + wholesaleSaved;
            } catch (Exception e) {
                log.error("[MarketPrice] 수집 실패 - itemCode: {}, kindCode: {}, error: {}",
                        cropCode.getItemCode(), cropCode.getKindCode(), e.getMessage());
            }
        }

        log.info("[MarketPrice] 가격 수집 완료 - 총 {}건 저장", totalSaved);
    }

    // ✅ 추가 - 날짜 범위 수집 (초기 수집용)
    @Transactional
    public void fetchAndSaveByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("[MarketPrice] 날짜 범위 수집 시작 - {} ~ {}", startDate, endDate);

        List<CropCode> cropCodes = cropCodeRepository.findAll();

        if (cropCodes.isEmpty()) {
            log.warn("[MarketPrice] CropCode 없음. /api/crops/sync 먼저 실행 필요");
            return;
        }

        int totalSaved = 0;

        for (CropCode cropCode : cropCodes) {
            try {
                int retailSaved = fetchAndSaveRange(cropCode,
                        MarketPrice.MarketType.RETAIL, startDate, endDate);  // ✅ 추가
                int wholesaleSaved = fetchAndSaveRange(cropCode,
                        MarketPrice.MarketType.WHOLESALE, startDate, endDate);  // ✅ 추가
                totalSaved += retailSaved + wholesaleSaved;
            } catch (Exception e) {
                log.error("[MarketPrice] 수집 실패 - itemCode: {}, kindCode: {}, error: {}",
                        cropCode.getItemCode(), cropCode.getKindCode(), e.getMessage());
            }
        }

        log.info("[MarketPrice] 날짜 범위 수집 완료 - 총 {}건 저장", totalSaved);
    }

    private int fetchAndSave(CropCode cropCode, MarketPrice.MarketType marketType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        MarketPriceRequestDto request = MarketPriceRequestDto.builder()
                .startDate(yesterday)
                .endDate(yesterday)
                .itemCategoryCode(cropCode.getItemCategoryCode())
                .itemCode(cropCode.getItemCode())
                .kindCode(cropCode.getKindCode())
                .rankCode(null)
                .marketType(marketType)
                .build();

        return execute(request, cropCode, marketType);  // ✅ 수정 - 공통 메서드로 분리
    }

    private int fetchAndSaveRange(CropCode cropCode, MarketPrice.MarketType marketType,
                                  LocalDate startDate, LocalDate endDate) {
        MarketPriceRequestDto request = MarketPriceRequestDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .itemCategoryCode(cropCode.getItemCategoryCode())
                .itemCode(cropCode.getItemCode())
                .kindCode(cropCode.getKindCode())
                .rankCode(null)
                .marketType(marketType)
                .build();

        return execute(request, cropCode, marketType);
    }

    // ✅ 추가 - fetchAndSave/fetchAndSaveRange 공통 로직
    private int execute(MarketPriceRequestDto request, CropCode cropCode,
                        MarketPrice.MarketType marketType) {
        KamisPriceResponseDto response = callKamisApi(request);

        if (response == null) {
            log.warn("[MarketPrice] 응답 null");
            return 0;
        }
        if (response.hasNoData()) {
            log.debug("[MarketPrice] 데이터 없음 - {} ({})",
                    cropCode.getItemName(), cropCode.getItemCode());
            return 0;
        }
        if (!response.isSuccess()) {
            log.warn("[MarketPrice] API 실패 - errorCode={}",
                    response.getData() != null
                            ? response.getData().getErrorCode() : "null");
            return 0;
        }
        if (response.getData() == null || response.getData().getItem() == null) {
            return 0;
        }

        List<MarketPrice> toSave = response.getData().getItem().stream()
                .filter(this::isValidItem)
                .map(item -> toEntity(item, cropCode, marketType))
                .filter(Objects::nonNull)
                .filter(mp -> !isDuplicate(mp))
                .collect(Collectors.toList());

        if (!toSave.isEmpty()) {
            marketPriceRepository.saveAll(toSave);
            log.info("[MarketPrice] 저장 - {} {} {}건",
                    cropCode.getItemName(), marketType, toSave.size());
        }

        return toSave.size();
    }

    // ─── KAMIS API 호출 ──────────────────────────────────────

    private KamisPriceResponseDto callKamisApi(MarketPriceRequestDto request) {
        try {
            return kamisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/service/price/xml.do")
                            .queryParam("action", request.getAction())
                            .queryParam("p_cert_key", certKey)
                            .queryParam("p_cert_id", certId)
                            .queryParam("p_returntype", "json")
                            .queryParam("p_startday", request.getFormattedStartDate())
                            .queryParam("p_endday", request.getFormattedEndDate())
                            .queryParam("p_itemcategorycode", request.getItemCategoryCode())
                            .queryParam("p_itemcode", request.getItemCode())
                            .queryParam("p_kindcode", request.getKindCode())
                            .queryParam("p_productrankcode", request.getRankCode())
                            .queryParam("p_countrycode", request.getRegionCode())
                            .queryParam("p_convert_kg_yn", request.getConvertKgYn())
                            .build())
                    .retrieve()
                    .bodyToMono(KamisPriceResponseDto.class)  // ← 변경
                    .block();
        } catch (Exception e) {
            log.error("[MarketPrice] API 호출 실패 - itemCode: {}, error: {}",
                    request.getItemCode(), e.getMessage());
            return null;
        }
    }

    // ─── 검증/변환 ──────────────────────────────────────────

    private boolean isValidItem(KamisItemDto item) {
        if (item.getItemname() == null || item.getItemname().isBlank()) return false;
        if (item.getMarketname() == null || item.getMarketname().isBlank()) return false;
        if (item.getPrice() == null || item.getPrice().isBlank()
                || item.getPrice().equals("-")) return false;
        if (item.getCountyname() != null &&
                (item.getCountyname().contains("평균") ||
                        item.getCountyname().contains("평년"))) return false;
        return true;
    }

    private boolean isDuplicate(MarketPrice mp) {
        return marketPriceRepository
                .existsByItemCodeAndKindCodeAndPriceDateAndMarketTypeAndRegionCodeAndMarketCode(
                        mp.getItemCode(), mp.getKindCode(),
                        mp.getPriceDate(), mp.getMarketType(),
                        mp.getRegionCode(), mp.getMarketCode());
    }

    private MarketPrice toEntity(KamisItemDto item, CropCode cropCode,
                                 MarketPrice.MarketType marketType) {
        Integer price = parsePrice(item.getPrice());
        if (price == null) return null;

        LocalDate priceDate = parseDate(item.getYyyy(), item.getRegday());
        if (priceDate == null) return null;

        return MarketPrice.builder()
                .itemCode(cropCode.getItemCode())
                .itemName(cropCode.getItemName())
                .kindCode(cropCode.getKindCode())
                .kindName(cropCode.getKindName())
                .rankCode(null)
                .regionCode(item.getCountyname())   // TODO: 추후 regionCode로 변경
                .regionName(item.getCountyname())
                .marketCode(item.getMarketname())   // TODO: 추후 marketCode로 변경
                .marketName(item.getMarketname())
                .price(price)
                .unit(
                marketType == MarketPrice.MarketType.RETAIL
                        ? cropCode.getRetailUnit()
                        : cropCode.getWholesaleUnit()
                )
                .priceDate(priceDate)
                .marketType(marketType)
                .build();
    }

    // 가격 파싱 (콤마 제거, 안전)
    private Integer parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank() || priceStr.equals("-")) {
            return null;
        }
        try {
            return Integer.parseInt(priceStr.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("[MarketPrice] 가격 파싱 실패: {}", priceStr);
            return null;
        }
    }

    // 날짜 파싱 (yyyy + MM/dd → LocalDate)
    private LocalDate parseDate(String yyyy, String regday) {
        if (yyyy == null || regday == null) return null;
        try {
            return LocalDate.parse(
                    yyyy + "-" + regday,
                    DateTimeFormatter.ofPattern("yyyy-MM/dd"));
        } catch (Exception e) {
            log.warn("[MarketPrice] 날짜 파싱 실패: {}-{}", yyyy, regday);
            return null;
        }
    }
}