package com.metaverse.growlab_be.ai.controller;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import com.metaverse.growlab_be.ai.dto.AiAdviceResponseDto;
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
}
