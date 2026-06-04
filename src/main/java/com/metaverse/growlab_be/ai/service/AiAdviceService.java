package com.metaverse.growlab_be.ai.service;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
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

    private final SensorLogService sensorLogService;

    public AiAdviceService(SensorLogService sensorLogService) {
        this.sensorLogService = sensorLogService;
    }

    public String getAdvice(AiAdviceRequestDto req) {
        String prompt = buildPrompt(req);

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "당신은 스마트팜 전문가입니다. 센서 데이터를 분석하여 식물 재배에 대한 간결하고 실용적인 조언을 한국어로 제공합니다. 마크다운 문법(###, **, * 등)을 절대 사용하지 마세요. 환경 전반, 조명 관리, 양액 시스템, 성장 속도 항목으로 나누어 각 항목당 1~2문장으로 답변하세요."),
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
        SensorLogRequestDto sensor = sensorLogService.getLatestData(req.getSerialNumber());

        double temperature = sensor != null && sensor.getTemperature() != null ? sensor.getTemperature()        : 0.0;
        double humidity    = sensor != null && sensor.getHumidity() != null ? sensor.getHumidity()           : 0.0;
        double ph          = sensor != null && sensor.getPh() != null ? sensor.getPh()                 : 0.0;
        double tds         = sensor != null && sensor.getTds() != null ? sensor.getTds()                : 0.0;
        double waterLevel  = sensor != null && sensor.getWater_level_status() != null
                ? (sensor.getWater_level_status() ? 100.0 : 0.0) : 0.0;

        return String.format("""
        식물 정보:
        - 품종: %s
        - 재배 일수: %d일
        - 생육 단계: %s
        
        상추(Lettuce) 재배 기준:
        - 적정 온도: 18~27°C
        - 적정 습도: 45~80%%
        - 적정 pH: 5.0~7.0
        - 적정 TDS: 800~1200ppm
        - 권장 조명 시간: 하루 14~17시간
        - 상추는 고온 환경에서 웃자람 및 생육 스트레스를 받을 수 있음
        - TDS가 너무 낮으면 영양 부족, 너무 높으면 비료 과다 상태일 수 있음
        
        현재 센서 데이터:
        - 온도: %.1f°C
        - 습도: %.1f%%
        - pH: %.1f
        - TDS: %.1fppm
        - 수위: %.1f%%
        
        아래 규칙에 따라 분석해주세요.
        
        [환경 전반]
        현재 재배 환경 상태를 전체적으로 평가하세요.
        
        [조명 관리]
        온도 및 생육 상태를 고려하여 조명 관련 조언을 제공하세요.
        
        [양액 시스템]
        pH, TDS, 수위 상태를 분석하고 필요한 조치를 설명하세요.
        
        [성장 속도]
        현재 생육 단계와 환경 기준을 기반으로 성장 상태를 평가하세요.
        
        규칙:
        - 각 항목은 1~2문장으로 작성
        - 불필요한 인사말 금지
        - 실제 스마트팜 운영자가 바로 적용 가능한 수준으로 설명
        - 수치가 적정 범위를 벗어나면 원인과 해결 방향 제시
        """,
                req.getSpeciesName()      != null ? req.getSpeciesName()      : "상추",
                req.getDaysSincePlanted() != null ? req.getDaysSincePlanted() : 0,
                req.getPlantStage()       != null ? req.getPlantStage()       : "미등록",
                temperature, humidity, ph, tds, waterLevel
        );
    }
}
