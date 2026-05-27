package com.metaverse.growlab_be.ai.service;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiAdviceService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String getAdvice(AiAdviceRequestDto req) {
        String prompt = buildPrompt(req);

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", "gpt-5.4-mini",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "당신은 스마트팜 전문가입니다. 센서 데이터를 분석하여 식물 재배에 대한 간결하고 실용적인 조언을 한국어로 제공합니다. 환경 전반, 조명 관리, 양액 시스템, 성장 속도 항목으로 나누어 각 항목당 1~2문장으로 답변하세요."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 500,
                "temperature", 0.7
        );

        Map response = client.post()
                .uri(apiUrl)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String buildPrompt(AiAdviceRequestDto req) {
        return String.format("""
                식물 정보:
                - 품종: %s
                - 재배 일수: %d일
                - 생육 단계: %s
                
                현재 센서 데이터:
                - 온도: %.1f°C (적정 범위: 18~28°C)
                - 습도: %.1f%% (적정 범위: 50~80%%)
                - pH: %.1f (적정 범위: 5.5~7.0)
                - EC: %.1f (적정 범위: 1.0~2.5)
                - 수위: %.1f%% (50%% 이상 유지 권장)
                
                위 데이터를 바탕으로 재배 조언을 해주세요.
                """,
                req.getSpeciesName() != null ? req.getSpeciesName() : "미등록",
                req.getDaysSincePlanted() != null ? req.getDaysSincePlanted() : 0,
                req.getPlantStage() != null ? req.getPlantStage() : "미등록",
                req.getTemperature() != null ? req.getTemperature() : 0.0,
                req.getHumidity() != null ? req.getHumidity() : 0.0,
                req.getPh() != null ? req.getPh() : 0.0,
                req.getEc() != null ? req.getEc() : 0.0,
                req.getWaterLevel() != null ? req.getWaterLevel() : 0.0
        );
    }
}
