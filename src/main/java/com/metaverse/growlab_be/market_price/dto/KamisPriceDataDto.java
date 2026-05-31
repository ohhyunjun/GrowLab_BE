package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KamisPriceDataDto {

    // 000=성공, 001=데이터없음, 200=파라미터오류, 900=인증오류
    @JsonProperty("error_code")
    private String errorCode;

    // 가격 데이터 목록
    @JsonProperty("item")
    private List<KamisItemDto> item;

    public boolean isSuccess() {
        return "000".equals(errorCode);
    }

    public boolean hasNoData() {
        return "001".equals(errorCode);
    }
}