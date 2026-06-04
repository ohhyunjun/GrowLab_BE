package com.metaverse.growlab_be.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAdviceRequestDto {
    private String  serialNumber;
    private String  speciesName;
    private Integer daysSincePlanted;
    private String  plantStage;
}
