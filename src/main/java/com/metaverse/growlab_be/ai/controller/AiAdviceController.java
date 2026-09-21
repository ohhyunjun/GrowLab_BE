package com.metaverse.growlab_be.ai.controller;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import com.metaverse.growlab_be.ai.dto.AiAdviceResponseDto;
import com.metaverse.growlab_be.ai.dto.PortDiagnosisRequestDto;
import com.metaverse.growlab_be.ai.service.AiAdviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAdviceController {

    private final AiAdviceService aiAdviceService;

    @PostMapping("/advice")
    public ResponseEntity<AiAdviceResponseDto> getAdvice(@RequestBody AiAdviceRequestDto request) {
        String advice = aiAdviceService.getAdvice(request);
        return ResponseEntity.ok(new AiAdviceResponseDto(advice));
    }

    // 포트 문제 LLM 정밀 분석 (버튼 클릭 시에만 호출 → 토큰 절약)
    @PostMapping("/port-diagnosis")
    public ResponseEntity<AiAdviceResponseDto> getPortDiagnosis(@RequestBody PortDiagnosisRequestDto request) {
        String diagnosis = aiAdviceService.getPortDiagnosis(request);
        return ResponseEntity.ok(new AiAdviceResponseDto(diagnosis));
    }
}
