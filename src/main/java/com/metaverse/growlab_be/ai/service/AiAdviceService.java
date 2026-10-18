package com.metaverse.growlab_be.ai.service;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import com.metaverse.growlab_be.ai.dto.PortDiagnosisRequestDto;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiAdviceService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final SensorLogService sensorLogService;
    private final SpeciesRepository speciesRepository;

    public AiAdviceService(SensorLogService sensorLogService, SpeciesRepository speciesRepository) {
        this.sensorLogService = sensorLogService;
        this.speciesRepository = speciesRepository;
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
                                "당신은 스마트팜 전문가입니다. 센서 데이터와 재배 중인 작물의 재배 기준을 비교 분석하여 " +
                                        "식물 재배에 대한 간결하고 실용적인 조언을 한국어로 제공합니다. " +
                                        "재배 기준이 '기준 미등록'으로 표시된 항목은 해당 작물에 대한 일반적인 재배 지식을 바탕으로 보완해서 판단하세요. " +
                                        "마크다운 문법(###, **, * 등)을 절대 사용하지 마세요. " +
                                        "환경 전반, 조명 관리, 양액 시스템, 성장 속도 항목으로 나누어 각 항목당 1~2문장으로 답변하세요."),
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

    // ── 포트 문제(성장 정체 등) LLM 정밀 분석. 규칙 1차 판정 결과를 근거로 원인·조치를 추정한다. (버튼 클릭 시에만 호출)
    public String getPortDiagnosis(PortDiagnosisRequestDto req) {
        SensorLogRequestDto sensor = sensorLogService.getLatestData(req.getSerialNumber());
        double temperature = sensor != null && sensor.getTemperature() != null ? sensor.getTemperature() : 0.0;
        double humidity    = sensor != null && sensor.getHumidity() != null ? sensor.getHumidity() : 0.0;
        double ph          = sensor != null && sensor.getPh() != null ? sensor.getPh() : 0.0;
        double tds         = sensor != null && sensor.getTds() != null ? sensor.getTds() : 0.0;
        double waterLevel  = sensor != null && sensor.getWater_level_status() != null
                ? (sensor.getWater_level_status() ? 100.0 : 0.0) : 0.0;

        Species species = null;
        if (req.getSpeciesId() != null) species = speciesRepository.findById(req.getSpeciesId()).orElse(null);
        if (species == null && req.getSpeciesName() != null && !req.getSpeciesName().isBlank())
            species = speciesRepository.findByName(req.getSpeciesName()).orElse(null);

        String speciesName = species != null ? species.getName()
                : (req.getSpeciesName() != null ? req.getSpeciesName() : "미등록 품종");
        String tempRange   = range(species != null ? species.getMinTemperature() : null, species != null ? species.getMaxTemperature() : null, "°C");
        String phRange     = range(species != null ? species.getMinPh() : null, species != null ? species.getMaxPh() : null, "");
        String tdsRange    = range(species != null ? species.getMinTds() : null, species != null ? species.getMaxTds() : null, "ppm");
        String note = species != null && species.getAiPromptGuideline() != null && !species.getAiPromptGuideline().isBlank()
                ? species.getAiPromptGuideline() : "등록된 생육 특이사항 없음";

        String sizeChangeText = (req.getSizeChangeDays() != null && req.getSizeChangePercent() != null)
                ? String.format("%d일 전보다 약 %+.0f%%", req.getSizeChangeDays(), req.getSizeChangePercent()) : "정보 없음";
        String speedText = "비교할 기록 부족";
        if (req.getSpeedVerdict() != null && req.getOwnRatePerDay() != null && req.getReferenceRatePerDay() != null) {
            String verdict = switch (req.getSpeedVerdict()) {
                case "faster" -> "더 빠름";
                case "slower" -> "더 느림";
                default -> "비슷함";
            };
            speedText = String.format("%s (이 포트 하루 약 %+.0f%%, 다른 재배 %s 하루 약 %+.0f%%)", verdict,
                    req.getOwnRatePerDay(),
                    req.getReferencePlantCount() != null ? req.getReferencePlantCount() + "개" : "",
                    req.getReferenceRatePerDay());
        }
        String sizeText = req.getCurrentSizePercent() != null ? String.format("%.1f%%", req.getCurrentSizePercent()) : "정보 없음";
        String diseaseText = (req.getDiseaseResult() != null && !req.getDiseaseResult().isBlank()) ? req.getDiseaseResult() : "질병 미검출";

        String prompt = String.format("""
        가정용 스마트팜의 한 포트 상태입니다. 아래 데이터를 근거로 이 포트의 상태를 진단하고,
        문제가 보이면 원인과 대응을 분석해 주세요.

        [식물]
        - 품종: %s
        - 재배 일수: %d일차
        - 생육 단계: %s

        [사진 관측] 사진 속 검출 박스 넓이 기준의 참고값이며 실제 크기를 잰 것이 아닙니다.
        - 이 포트의 과거 대비 크기: %s
        - 현재 크기(화면 대비): %s
        - 같은 품종의 다른 재배가 심은 뒤 같은 시기에 보인 성장 속도와 비교: %s
        - 질병 라벨: %s

        [최근 센서]
        - 온도: %.1f°C (기준 %s)
        - 습도: %.1f%%
        - pH: %.1f (기준 %s)
        - TDS: %.1fppm (기준 %s)
        - 수위: %.1f%%
        - 생육 특이사항: %s

        '감지된 문제', '가능한 원인', '권장 조치' 세 항목으로 나눠 각 1~3줄로 한국어로 답하세요.
        수확일이나 수확까지 남은 일수는 추정하지 마세요. 수확기 도달일은 별도의 영상 예측 결과로 안내합니다.
        마크다운 기호(#, *, - 등)는 쓰지 마세요. '정보 없음'이나 '비교할 기록 부족'인 항목을 근거로
        성장이 느리다고 단정하지 마세요. 확정 진단이 아니라 참고 추정임을 전제로,
        현장에서 바로 실행 가능한 구체적 조치 위주로 작성하세요.
        """,
                speciesName, req.getDaysSincePlanted() != null ? req.getDaysSincePlanted() : 0,
                req.getPlantStage() != null ? req.getPlantStage() : "미등록",
                sizeChangeText, sizeText, speedText, diseaseText,
                temperature, tempRange, humidity, ph, phRange, tds, tdsRange, waterLevel, note);

        String system = "당신은 스마트팜 작물 진단 전문가입니다. 주어진 센서·성장 데이터로 성장 저해나 이상의 원인을 추정하고 "
                + "실용적인 조치를 제안합니다. 마크다운을 쓰지 말고, 간결하고 단정적이지 않은 한국어로 답하세요.";
        return callOpenAi(system, prompt, 450);
    }

    // OpenAI Chat Completions 호출 공통부.
    private String callOpenAi(String systemMessage, String userPrompt, int maxTokens) {
        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemMessage),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.5
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

        double temperature = sensor != null && sensor.getTemperature() != null ? sensor.getTemperature() : 0.0;
        double humidity    = sensor != null && sensor.getHumidity() != null ? sensor.getHumidity()       : 0.0;
        double ph          = sensor != null && sensor.getPh() != null ? sensor.getPh()                   : 0.0;
        double tds         = sensor != null && sensor.getTds() != null ? sensor.getTds()                 : 0.0;
        double waterLevel  = sensor != null && sensor.getWater_level_status() != null
                ? (sensor.getWater_level_status() ? 100.0 : 0.0) : 0.0;

        Species species = findSpecies(req);

        String speciesName    = species != null ? species.getName() : (req.getSpeciesName() != null ? req.getSpeciesName() : "미등록 품종");
        String categoryText   = species != null ? categoryToKorean(species.getCategory()) : "정보 없음";
        String matureText     = species != null ? species.getDaysToMature() + "일" : "정보 없음";

        String tempRange   = range(species != null ? species.getMinTemperature() : null, species != null ? species.getMaxTemperature() : null, "°C");
        String humidRange  = range(species != null ? species.getMinHumidity() : null, species != null ? species.getMaxHumidity() : null, "%");
        String phRange     = range(species != null ? species.getMinPh() : null, species != null ? species.getMaxPh() : null, "");
        String tdsRange    = range(species != null ? species.getMinTds() : null, species != null ? species.getMaxTds() : null, "ppm");
        String lightRange  = range(species != null ? species.getMinLightHours() : null, species != null ? species.getMaxLightHours() : null, "시간");

        String note = species != null && species.getAiPromptGuideline() != null && !species.getAiPromptGuideline().isBlank()
                ? species.getAiPromptGuideline()
                : "등록된 생육 특이사항 없음";

        return String.format("""
        식물 정보:
        - 품종: %s
        - 분류: %s
        - 재배 일수: %d일
        - 생육 단계: %s
        - 평균 성숙 기간: %s

        %s(%s) 재배 기준:
        - 적정 온도: %s
        - 적정 습도: %s
        - 적정 pH: %s
        - 적정 TDS: %s
        - 권장 조명 시간: %s
        - 생육 특이사항: %s

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
        현재 생육 단계와 평균 성숙 기간을 기반으로 성장 상태를 평가하세요.

        규칙:
        - 각 항목은 1~2문장으로 작성
        - 불필요한 인사말 금지
        - 실제 스마트팜 운영자가 바로 적용 가능한 수준으로 설명
        - 수치가 적정 범위를 벗어나면 원인과 해결 방향 제시
        - "기준 미등록"인 항목은 해당 작물의 일반적인 재배 지식으로 보완해서 판단
        """,
                speciesName, categoryText, req.getDaysSincePlanted() != null ? req.getDaysSincePlanted() : 0,
                req.getPlantStage() != null ? req.getPlantStage() : "미등록", matureText,
                speciesName, categoryText,
                tempRange, humidRange, phRange, tdsRange, lightRange, note,
                temperature, humidity, ph, tds, waterLevel
        );
    }

    // min/max 중 하나라도 있으면 범위 문자열 생성, 둘 다 없으면 "기준 미등록"
    private String range(Double min, Double max, String unit) {
        if (min == null && max == null) return "기준 미등록";
        if (min != null && max != null) return trim(min) + "~" + trim(max) + unit;
        if (min != null) return trim(min) + unit + " 이상";
        return trim(max) + unit + " 이하";
    }

    private String trim(Double value) {
        // 20.0 -> "20", 20.5 -> "20.5"
        if (value == Math.floor(value)) {
            return String.valueOf(value.intValue());
        }
        return String.valueOf(value);
    }

    private Species findSpecies(AiAdviceRequestDto req) {
        Optional<Species> found = Optional.empty();

        if (req.getSpeciesId() != null) {
            found = speciesRepository.findById(req.getSpeciesId());
        }
        if (found.isEmpty() && req.getSpeciesName() != null && !req.getSpeciesName().isBlank()) {
            found = speciesRepository.findByName(req.getSpeciesName());
        }

        return found.orElse(null);
    }

    private String categoryToKorean(com.metaverse.growlab_be.species.domain.Category category) {
        if (category == null) return "정보 없음";
        return switch (category) {
            case VEGETABLE -> "채소";
            case FRUIT -> "과일";
            case HERB -> "허브";
            case ORNAMENTAL -> "관상식물";
        };
    }
}