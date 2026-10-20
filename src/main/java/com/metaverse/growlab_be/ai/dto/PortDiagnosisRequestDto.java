package com.metaverse.growlab_be.ai.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 포트 문제(성장 정체 등) LLM 정밀 분석 요청.
 * 사진 관측(이 포트의 과거 대비 크기, 같은 시기 다른 재배와의 속도 비교)과 현재 상태를 함께 넘겨,
 * LLM이 원인·조치를 추정하게 한다.
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
    private Integer sizeChangeDays;       // 며칠 전과 견줬는지, null 가능
    private Double  sizeChangePercent;    // 그날보다 몇 % 커졌는지(이 포트 자신의 크기), null 가능
    private String  speedVerdict;         // 같은 품종 다른 재배의 같은 시기 속도와 비교: faster·similar·slower, null 가능
    private Double  ownRatePerDay;        // 이 포트의 하루 성장률(%), null 가능
    private Double  referenceRatePerDay;  // 다른 재배들의 같은 시기 하루 성장률(%), null 가능
    private Integer referencePlantCount;  // 비교한 다른 재배 기록 수, null 가능
    // 화면 대비 크기. 포트가 찍히는 자리에 따라 달라지는 값이라 크다/작다의 근거가 못 되어 지시문에는 넣지 않는다.
    private Double  currentSizePercent;
    private String  diseaseResult;     // 질병 라벨(있으면)
}
