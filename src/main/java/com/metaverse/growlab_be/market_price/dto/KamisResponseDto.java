package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)

public class KamisResponseDto {

    @JsonProperty("condition")
    private Object condition;

    // API 성공/실패 확인용
    // 000=Success, 001=no data, 200=Wrong Parameters, 900=Unauthenticated
    @JsonProperty("error_code")
    private String errorCode;

    // data.item 구조가 아니라 최상위 info 배열로 옴
    @JsonProperty("info")
    private List<KamisItemDto> info;

    // 성공 여부 확인
    public boolean isSuccess() {
        return "000".equals(errorCode);
    }

    // 데이터 없음 여부 확인
    public boolean hasNoData() {
        return "001".equals(errorCode);
    }
}
