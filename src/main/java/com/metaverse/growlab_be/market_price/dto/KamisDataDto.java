package com.metaverse.growlab_be.market_price.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KamisDataDto {

    @JsonProperty("item")
    private List<KamisItemDto> item;
}
