package com.metaverse.growlab_be.ai.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 포트 문제(성장 정체 등) LLM 정밀 분석 요청.
 * 규칙 기반 1차 판정 결과(gap/rank)와 현재 상태를 함께 넘겨, LLM이 원인·조치를 추정하게 한다.
 * 버튼 클릭 시에만 호출된다(토큰 절약).
 */
@Getter
@Setter
public class PortDiagnosisRequestDto {
    private String  serialNumber;
    private Long    speciesId;
    private String  speciesName;
    private Integer portIndex;
    private Integer daysSincePlanted;
    private String  plantStage;
    private Double  gapPercent;        // 또래 대비 성장 격차(%) — 음수면 느림
    private Integer rank;              // 또래 중 순위 (1 = 가장 큼)
    private Integer portCount;         // 비교된 포트 수
    private Double  currentSizePercent;// 현재 크기(화면 대비 %), null 가능
    private String  diseaseResult;     // 질병 라벨(있으면)
}
