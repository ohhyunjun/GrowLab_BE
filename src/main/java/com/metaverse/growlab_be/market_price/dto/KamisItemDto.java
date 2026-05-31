package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KamisItemDto {

// ─── productInfo API 응답 필드 ───────────────────────
    // action=productInfo (품목코드 수집용)

    @JsonProperty("itemcategorycode")
    private String itemcategorycode;            // 부류코드

    @JsonProperty("itemcategoryname")
    private String itemcategoryname;           // 부류명

    @JsonProperty("itemcode")
    private String itemcode;                    // 품목코드

    @JsonProperty("itemname")
    private String itemname;                    // 품목명

    @JsonProperty("kindcode")
    private String kindcode;                    // 품종코드

    @JsonProperty("kindname")
    private String kindname;                    // 품종명

    @JsonProperty("wholesale_unit")
    private Object wholesale_unit;              // 도매 출하단위

    @JsonProperty("wholesale_unitsize")
    private Object wholesale_unitsize;          // 도매 출하단위 크기

    @JsonProperty("retail_unit")
    private Object retail_unit;                 // 소매 출하단위

    @JsonProperty("retail_unitsize")
    private Object retail_unitsize;             // 소매 출하단위 크기

    @JsonProperty("eco_unit")
    private Object eco_unit;                    // 친환경 출하단위

    @JsonProperty("eco_unitsize")
    private Object eco_unitsize;                // 친환경 출하단위 크기

    @JsonProperty("whole_productrankcode")
    private Object whole_productrankcode;       // 도매 등급코드

    @JsonProperty("retail_productrankcode")
    private Object retail_productrankcode;      // 소매 등급코드

    @JsonProperty("new_natreu_productrankcode")
    private Object new_natreu_productrankcode;  // 친환경 등급코드

    // ─── 가격 조회 API 응답 필드 ─────────────────────────
    // action=periodRetailProductList / periodWholesaleProductList

    @JsonProperty("countyname")
    private String countyname;                  // 지역명

    @JsonProperty("marketname")
    private String marketname;                  // 시장명

    @JsonProperty("yyyy")
    private String yyyy;                        // 연도

    @JsonProperty("regday")
    private String regday;                      // 가격 등록 날짜 (MM/dd)

    @JsonProperty("price")
    private String price;                       // 가격

    // ─── Object → String 변환 getter ─────────────────────
    // 빈 배열 [] → null, String → String 그대로 반환

    public String getWholesale_unit() {
        return toStringOrNull(wholesale_unit);
    }

    public String getWholesale_unitsize() {
        return toStringOrNull(wholesale_unitsize);
    }

    public String getRetail_unit() {
        return toStringOrNull(retail_unit);
    }

    public String getRetail_unitsize() {
        return toStringOrNull(retail_unitsize);
    }

    public String getEco_unit() {
        return toStringOrNull(eco_unit);
    }

    public String getEco_unitsize() {
        return toStringOrNull(eco_unitsize);
    }

    public String getWhole_productrankcode() {
        return toStringOrNull(whole_productrankcode);
    }

    public String getRetail_productrankcode() {
        return toStringOrNull(retail_productrankcode);
    }

    public String getNew_natreu_productrankcode() {
        return toStringOrNull(new_natreu_productrankcode);
    }

    // 빈 배열 [] → null 반환, String → String 반환
    private String toStringOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        return null;
    }
}
