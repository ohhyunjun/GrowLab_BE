package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KamisResponseDto {

    @JsonProperty("data")
    private KamisData data;

    @Getter
    @Setter
    public static class KamisData {
        @JsonProperty("item")
        private List<KamisItem> item;
    }

    @Getter
    @Setter
    public static class KamisItem {
        @JsonProperty("itemname")
        private String itemname; // 품목명

        @JsonProperty("kindname")
        private String kindname; // 품목 세부명

        @JsonProperty("countyname")
        private String countyname; // 지역명

        @JsonProperty("marketname")
        private String marketname; // 시장명

        @JsonProperty("yyyy")
        private String yyyy; // 가격조사 년도

        @JsonProperty("regday")
        private String regday; // 가격조사 일자

        @JsonProperty("price")
        private String price; // 가격
    }
}
