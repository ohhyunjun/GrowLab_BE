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
        @JsonProperty("item_name")
        private String itemName;

        @JsonProperty("dpr1")
        private String wholesalePrice;

        @JsonProperty("dpr2")
        private String retailPrice;

        @JsonProperty("kind_name")
        private String kindName;

        @JsonProperty("yyyy")
        private String year;

        @JsonProperty("regday")
        private String regDay;
    }
}
