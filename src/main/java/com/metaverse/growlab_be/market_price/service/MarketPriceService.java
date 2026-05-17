package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.MarketPrice;
import com.metaverse.growlab_be.market_price.dto.KamisResponseDto;
import com.metaverse.growlab_be.market_price.dto.MarketPriceResponseDto;
import com.metaverse.growlab_be.market_price.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;
    private final WebClient kamisWebClient;

    /** KAMIS에서 DB에 추가할 TARGET_ITEMS 목록을 만들기.
    TODO: [확장성] 나중에 방울토마토, 바질 등 새로운 작물이 추가되면 아래 리스트에 이름표만 추가할 것
    예: List.of("상추", "방울토마토", "바질");
    **/
    private static final List<String> TARGET_ITEMS = List.of("상추");

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

        // TODO: KAMIS에서 인증키 발급받으면 여기에 대입
        String certKey = "YOUR_KAMIS_API_KEY_HERE";
        String certId = "YOUR_KAMIS_ID_HERE"; // KAMIS는 요청시 ID(이메일이나 회원ID)를 요구할 수 있습니다.

        try {
            // 1. 외부 API 호출 및 응답 받기
            KamisResponseDto response = kamisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/service/price/xml.do")
                            .queryParam("action", "periodProductList")
                            .queryParam("p_cert_key", certKey)
                            .queryParam("p_cert_id", certId)
                            .queryParam("p_returntype", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(KamisResponseDto.class)
                    .block();

            // 2. 받아온 데이터가 가득 차 있다면 반복문 돌며 DB에 저장
            if (response != null && response.getData() != null && response.getData().getItem() != null) {
                for (KamisResponseDto.KamisItem item : response.getData().getItem()) {

                    if (TARGET_ITEMS.contains(item.getItemName())) {

                        // 주말/공휴일 등 가격 데이터가 없어서 "-"로 넘어오는 경우 패스
                        if ("-".equals(item.getWholesalePrice()) || "-".equals(item.getRetailPrice())) {
                            log.warn("⚠️ {}의 가격 데이터가 존재하지 않는 날짜입니다. (공휴일/주말 가능성)", item.getItemName());
                            continue;
                        }

                        // 콤마(,) 제거 후 숫자로 변환
                        int wholesale = Integer.parseInt(item.getWholesalePrice().replace(",", ""));
                        int retail = Integer.parseInt(item.getRetailPrice().replace(",", ""));

                        MarketPrice marketPrice = MarketPrice.builder()
                                .itemName(item.getItemName())
                                .wholesalePrice(wholesale)
                                .retailPrice(retail)
                                .priceUnit("1kg")
                                .priceDate(LocalDate.now())
                                .build();

                        marketPriceRepository.save(marketPrice);
                        log.info("📢 KAMIS 데이터 DB 저장 완료: {} (도매:{}, 소매:{})", item.getItemName(), wholesale, retail);
                    }
                }
            }

        } catch (Exception e) {
            log.error("KAMIS API 데이터 파싱 및 저장 중 에러 발생: ", e);
        }
    }
}