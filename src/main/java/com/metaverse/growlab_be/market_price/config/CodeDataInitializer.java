package com.metaverse.growlab_be.market_price.config;

import com.metaverse.growlab_be.market_price.domain.RankCode;
import com.metaverse.growlab_be.market_price.domain.RegionCode;
import com.metaverse.growlab_be.market_price.repository.RankCodeRepository;
import com.metaverse.growlab_be.market_price.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeDataInitializer implements CommandLineRunner {

    private final RankCodeRepository rankCodeRepository;
    private final RegionCodeRepository regionCodeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initRankCodes();
        initRegionCodes();
    }

    // 등급코드 초기 데이터

    private void initRankCodes() {
        if (rankCodeRepository.count() > 0) {
            log.info("[CodeInit] 등급코드 이미 존재 - 스킵");
            return;
        }

        List<RankCode> rankCodes = List.of(
                RankCode.builder().code("01").name("1등급").build(),
                RankCode.builder().code("02").name("2등급").build(),
                RankCode.builder().code("03").name("3등급").build(),
                RankCode.builder().code("04").name("상품").build(),
                RankCode.builder().code("05").name("중품").build(),
                RankCode.builder().code("06").name("하품").build(),
                RankCode.builder().code("07").name("유기농").build(),
                RankCode.builder().code("08").name("무농약").build(),
                RankCode.builder().code("09").name("저농약").build(),
                RankCode.builder().code("10").name("냉장").build(),
                RankCode.builder().code("11").name("냉동").build(),
                RankCode.builder().code("12").name("무항생제").build(),
                RankCode.builder().code("13").name("S과(감귤도매)").build(),
                RankCode.builder().code("14").name("M과(감귤도매)").build(),
                RankCode.builder().code("15").name("M과(감귤소매)").build(),
                RankCode.builder().code("16").name("S과(감귤소매)").build(),
                RankCode.builder().code("17").name("1+등급").build(),
                RankCode.builder().code("18").name("동물복지란").build(),
                RankCode.builder().code("19").name("특大").build(),
                RankCode.builder().code("20").name("大").build(),
                RankCode.builder().code("21").name("中").build(),
                RankCode.builder().code("22").name("小").build(),
                RankCode.builder().code("23").name("2L과(포도)").build(),
                RankCode.builder().code("24").name("L과(포도)").build(),
                RankCode.builder().code("25").name("M과(포도)").build(),
                RankCode.builder().code("26").name("S과(포도)").build(),
                RankCode.builder().code("27").name("대멸").build(),
                RankCode.builder().code("28").name("중멸").build(),
                RankCode.builder().code("29").name("세멸").build()
        );

        rankCodeRepository.saveAll(rankCodes);
        log.info("[CodeInit] 등급코드 {}건 저장 완료", rankCodes.size());
    }

    // 지역코드 초기 데이터

    private void initRegionCodes() {
        if (regionCodeRepository.count() > 0) {
            log.info("[CodeInit] 지역코드 이미 존재 - 스킵");
            return;
        }

        List<RegionCode> regionCodes = List.of(

                // 소매 지역 (productClsCode = "01")
                RegionCode.builder().regionCode("1101").regionName("서울").marketCode("1101").marketName("서울").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2100").regionName("부산").marketCode("2100").marketName("부산").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2200").regionName("대구").marketCode("2200").marketName("대구").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2300").regionName("인천").marketCode("2300").marketName("인천").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2401").regionName("광주").marketCode("2401").marketName("광주").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2501").regionName("대전").marketCode("2501").marketName("대전").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2601").regionName("울산").marketCode("2601").marketName("울산").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("2701").regionName("세종").marketCode("2701").marketName("세종").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3111").regionName("경기").marketCode("3111").marketName("수원").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3112").regionName("경기").marketCode("3112").marketName("성남").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3113").regionName("경기").marketCode("3113").marketName("의정부").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3138").regionName("경기").marketCode("3138").marketName("고양").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3145").regionName("경기").marketCode("3145").marketName("용인").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3211").regionName("강원").marketCode("3211").marketName("춘천").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3214").regionName("강원").marketCode("3214").marketName("강릉").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3311").regionName("충북").marketCode("3311").marketName("청주").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3411").regionName("충남").marketCode("3411").marketName("천안").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3511").regionName("전북").marketCode("3511").marketName("전주").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3512").regionName("전북").marketCode("3512").marketName("군산").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3611").regionName("전남").marketCode("3611").marketName("목포").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3613").regionName("전남").marketCode("3613").marketName("순천").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3711").regionName("경북").marketCode("3711").marketName("포항").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3714").regionName("경북").marketCode("3714").marketName("안동").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3811").regionName("경남").marketCode("3811").marketName("마산").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3814").regionName("경남").marketCode("3814").marketName("창원").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3818").regionName("경남").marketCode("3818").marketName("김해").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),
                RegionCode.builder().regionCode("3911").regionName("제주").marketCode("3911").marketName("제주").marketType(RegionCode.MarketType.RETAIL).productClsCode("01").build(),

                // 온라인 (productClsCode = "09")
                RegionCode.builder().regionCode("9998").regionName("온라인").marketCode("9998").marketName("온라인").marketType(RegionCode.MarketType.ONLINE).productClsCode("09").build(),

                //  도매시장 (productClsCode = "02")
                RegionCode.builder().regionCode("1101").regionName("서울").marketCode("0110111").marketName("가락도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build(),
                RegionCode.builder().regionCode("1101").regionName("서울").marketCode("0110114").marketName("강서도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build(),
                RegionCode.builder().regionCode("2100").regionName("부산").marketCode("0210111").marketName("엄궁도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build(),
                RegionCode.builder().regionCode("2200").regionName("대구").marketCode("0220111").marketName("대구도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build(),
                RegionCode.builder().regionCode("2401").regionName("광주").marketCode("0240111").marketName("각화도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build(),
                RegionCode.builder().regionCode("2501").regionName("대전").marketCode("0250111").marketName("오정도매").marketType(RegionCode.MarketType.WHOLESALE).productClsCode("02").build()
        );

        regionCodeRepository.saveAll(regionCodes);
        log.info("[CodeInit] 지역코드 {}건 저장 완료", regionCodes.size());
    }
}
