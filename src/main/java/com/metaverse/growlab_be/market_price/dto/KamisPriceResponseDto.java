package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KamisPriceResponseDto {

    // 요청 파라미터 그대로 돌려줌 → 무시
    @JsonProperty("condition")
    private Object condition;

    // 실제 데이터
    @JsonProperty("data")
    private KamisPriceDataDto data;

    public boolean isSuccess() {
        return data != null && data.isSuccess();
    }

    public boolean hasNoData() {
        return data != null && data.hasNoData();
    }
}