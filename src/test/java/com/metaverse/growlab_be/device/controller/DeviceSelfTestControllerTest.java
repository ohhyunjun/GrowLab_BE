package com.metaverse.growlab_be.device.controller;

import com.metaverse.growlab_be.device.dto.SelfTestReportRequestDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class DeviceSelfTestControllerTest {

    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final DeviceSelfTestController controller = new DeviceSelfTestController(devices);

    @Test
    void logsBothOkAndFailReportsWithTheirFullDetails(CapturedOutput output) {
        when(devices.existsById("GROWLAB-G111")).thenReturn(true);
        for (String status : List.of("OK", "FAIL")) {
            var report = new SelfTestReportRequestDto(
                    "GROWLAB-G111", "1".repeat(32), "scheduled",
                    "2026-10-06T00:00:00+00:00", status,
                    Map.of("PH", "OK", "TDS", "OK", "DHT", "OK", "FLOAT", "OK",
                           "ARDUINO", "OK", "CAM", status.equals("OK") ? "OK" : "FAIL",
                           "CAM_DETAIL", status.equals("OK") ? "OK" : "CAPTURE_FAIL"),
                    status.equals("OK") ? List.of() : List.of("CAM"));
            assertEquals(200, controller.receive(report).getStatusCode().value());
        }
        assertTrue(output.getOut().contains("GROWLAB_SELFTEST serial=GROWLAB-G111"));
        assertTrue(output.getOut().contains("status=OK"));
        assertTrue(output.getOut().contains("status=FAIL"));
        assertTrue(output.getOut().contains("PH=OK"));
        assertTrue(output.getOut().contains("CAM=FAIL"));
    }

    @Test
    void rejectsUnknownDeviceAndInvalidTimestamp() {
        var report = new SelfTestReportRequestDto(
                "UNKNOWN", "2".repeat(32), "scheduled", "bad-time", "OK",
                Map.of("PH", "OK"), List.of());
        assertEquals(400, controller.receive(report).getStatusCode().value());
        var validTime = new SelfTestReportRequestDto(
                "UNKNOWN", "2".repeat(32), "scheduled", "2026-10-06T00:00:00Z", "OK",
                Map.of("PH", "OK"), List.of());
        assertEquals(400, controller.receive(validTime).getStatusCode().value());
    }

    @Test
    void acceptsTheRpiJsonOverHttp(CapturedOutput output) throws Exception {
        when(devices.existsById("GROWLAB-G111")).thenReturn(true);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/device-selftests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"GROWLAB-G111","requestId":"11111111111111111111111111111111",
                                 "trigger":"scheduled","timestamp":"2026-10-06T00:00:00+00:00",
                                 "status":"OK","detail":{"PH":"OK","TDS":"OK","DHT":"OK",
                                 "FLOAT":"OK","ARDUINO":"OK","CAM":"OK","CAM_DETAIL":"OK"},
                                 "failed":[]}
                                """))
                .andExpect(status().isOk());
        assertTrue(output.getOut().contains("GROWLAB_SELFTEST serial=GROWLAB-G111"));
    }
}
