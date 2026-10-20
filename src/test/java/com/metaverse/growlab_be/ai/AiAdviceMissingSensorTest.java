package com.metaverse.growlab_be.ai;

import com.metaverse.growlab_be.ai.dto.AiAdviceRequestDto;
import com.metaverse.growlab_be.ai.service.AiAdviceService;
import com.metaverse.growlab_be.sensor_log.dto.SensorLogRequestDto;
import com.metaverse.growlab_be.sensor_log.service.SensorLogService;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiAdviceMissingSensorTest {
    private final SensorLogService sensors = mock(SensorLogService.class);
    private final SpeciesRepository species = mock(SpeciesRepository.class);
    private final AiAdviceService service = new AiAdviceService(sensors, species);

    @Test void noSensorDataDoesNotCallTheLanguageModelWithInventedZeros() {
        AiAdviceRequestDto request = new AiAdviceRequestDto();
        request.setSerialNumber("TEST");
        when(sensors.getLatestData("TEST")).thenReturn(null);

        assertThat(service.getAdvice(request)).contains("센서 데이터가 아직 없어");
        verifyNoInteractions(species);
    }

    @Test void partialSensorDataNamesMissingMeasurementsInsteadOfZero() {
        AiAdviceRequestDto request = new AiAdviceRequestDto();
        request.setSerialNumber("TEST");
        request.setSpeciesName("상추");
        SensorLogRequestDto sensor = new SensorLogRequestDto();
        ReflectionTestUtils.setField(sensor, "temperature", 20.0f);
        when(species.findByName("상추")).thenReturn(Optional.empty());

        String prompt = ReflectionTestUtils.invokeMethod(service, "buildPrompt", request, sensor);
        assertThat(prompt).contains("온도: 20.0°C", "습도: 측정 없음", "pH: 측정 없음",
                "TDS: 측정 없음", "수위: 측정 없음");
        assertThat(prompt).doesNotContain("습도: 0.0", "pH: 0.0", "TDS: 0.0", "수위: 0.0");
    }
}
