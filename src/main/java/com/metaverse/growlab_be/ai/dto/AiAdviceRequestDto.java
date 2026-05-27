package com.metaverse.growlab_be.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAdviceRequestDto {
    private String serialNumber;
    private String speciesName;
    private Double temperature;
    private Double humidity;
    private Double ph;
    private Double ec;
    private Double waterLevel;
    private Integer daysSincePlanted;
    private String plantStage;
}
