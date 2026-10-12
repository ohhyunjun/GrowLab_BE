package com.metaverse.growlab_be.ai.controller;

import com.metaverse.growlab_be.ai.dto.PortDiagnosisRequestDto;
import com.metaverse.growlab_be.ai.service.AiAdviceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiAdviceControllerTest {

    @Test
    void acceptsTheAwsFrontendPortDiagnosisRequest() throws Exception {
        var service = mock(AiAdviceService.class);
        when(service.getPortDiagnosis(org.mockito.ArgumentMatchers.any(PortDiagnosisRequestDto.class)))
                .thenReturn("포트 진단 결과");

        MockMvcBuilders.standaloneSetup(new AiAdviceController(service)).build()
                .perform(post("/api/ai/port-diagnosis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"GROWLAB-G111","speciesId":1,"speciesName":"상추",
                                 "portIndex":3,"daysSincePlanted":12,"plantStage":"발아",
                                 "gapPercent":-15.5,"rank":4,"portCount":8,
                                 "currentSizePercent":2.1,"diseaseResult":"질병 미검출"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advice").value("포트 진단 결과"));

        verify(service).getPortDiagnosis(argThat(req ->
                "GROWLAB-G111".equals(req.getSerialNumber())
                        && req.getPortIndex() == 3
                        && req.getGapPercent() == -15.5));
    }
}
