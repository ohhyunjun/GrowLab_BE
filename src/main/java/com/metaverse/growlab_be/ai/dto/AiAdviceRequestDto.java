package com.metaverse.growlab_be.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAdviceRequestDto {
    private String  serialNumber;
    private Long    speciesId;       // ✅ 있으면 우선 사용 (더 정확)
    private String  speciesName;     // speciesId가 없을 때 이름으로 폴백 조회
    private Integer daysSincePlanted;
    private String  plantStage;
}
