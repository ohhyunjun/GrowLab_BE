package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.KamisResponseDto;
import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;
    private final WebClient kamisWebClient;

    // 💡 application.properties에서 설정한 키값들을 가져와서 사용할 수 있도록 @Value 어노테이션 활용
    @Value("${kamis.api.key:YOUR_KAMIS_API_KEY_HERE}")
    private String certKey;

    @Value("${kamis.api.id:YOUR_KAMIS_ID_HERE}")
    private String certId;

    /**
     * KAMIS에서 DB에 추가할 TARGET_ITEMS 목록을 만들기.
     * TODO: [확장성] 나중에 방울토마토, 바질 등 새로운 작물이 추가되면 아래 리스트에 이름표만 추가할 것
     * 예: List.of("상추", "방울토마토", "바질");
     **/
    private static final List<String> TARGET_ITEMS = List.of("방울토마토", "청상추", "적상추", "바질", "딸기",
            "파프리카", "브로콜리", "고추", "블루베리", "페퍼민트", "청경채");

    private static final Map<String, String> ITEM_CODES = Map.of(
            "방울토마토", "225",
            "청상추", "141",
            "적상추", "141",
            "딸기", "226",
            "파프리카", "244",
            "브로콜리", "152",
            "고추", "213"
    );

    // 1. 특정 품목의 가장 최신 도소매 가격 조회
    public MarketPriceResponseDto getLatestPrice(String itemName) {
        MarketPrice marketPrice = marketPriceRepository.findFirstByItemNameOrderByPriceDateDesc(itemName)
                .orElseThrow(() -> new IllegalArgumentException("해당 품목의 가격 데이터가 존재하지 않습니다: " + itemName));

        return new MarketPriceResponseDto(marketPrice);
    }

    // 2. 특정 품목의 최근 일주일(7일)간 가격 추이 조회
    public List<MarketPriceResponseDto> getWeeklyPriceMovement(String itemName) {
        // 오늘 날짜 기준으로 6일 전부터 오늘까지 총 7일간의 데이터를 타겟으로 잡음
        LocalDate startDate = LocalDate.now().minusDays(6);

        List<MarketPrice> prices = marketPriceRepository
                .findByItemNameAndPriceDateGreaterThanEqualOrderByPriceDateAsc(itemName, startDate);

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환 (.stream() 활용)
        return prices.stream()
                .map(MarketPriceResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 외부 오픈 API(KAMIS) 호출 및 DB 저장 로직
    @Transactional
    public void fetchAndSaveMarketPrice() {
        log.info("=== KAMIS 외부 농산물 가격 API 호출 시작 ===");
        log.info("사용 중인 KAMIS ID: {}", certId); // 보안상 ID 정도만 로그로 검증

        for (Map.Entry<String, String> entry : ITEM_CODES.entrySet()) {

            String itemName = entry.getKey();
            String itemCode = entry.getValue();

            log.info("=== {} 가격 조회 시작 ===", itemName);

            String responseBody = kamisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/service/price/xml.do")
                            .queryParam("action", "periodProductList")
                            .queryParam("p_cert_key", certKey)
                            .queryParam("p_cert_id", certId)
                            .queryParam("p_itemcode", itemCode)
                            .queryParam("p_returntype", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("{} 응답 = {}", itemName, responseBody);
        }
    }
}