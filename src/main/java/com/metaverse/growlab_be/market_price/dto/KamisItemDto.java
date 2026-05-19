package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KamisItemDto {

    @JsonProperty("itemname")
    private String itemname;    //  품목명

    @JsonProperty("kindname")
    private String kindname;    // 품목유형명

    @JsonProperty("countyname")
    private String countyname;  // 생산자명

    @JsonProperty("marketname")
    private String marketname;  // 시장명

    @JsonProperty("yyyy")
    private String yyyy;    // 년도

    @JsonProperty("regday")
    private String regday;    // 가격 등록 날짜

    @JsonProperty("price")
    private String price;   // 가격
}
