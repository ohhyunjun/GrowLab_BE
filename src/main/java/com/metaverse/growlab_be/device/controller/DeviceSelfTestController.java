package com.metaverse.growlab_be.device.controller;

import com.metaverse.growlab_be.device.dto.SelfTestReportRequestDto;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeviceSelfTestController {

    private static final Set<String> REQUIRED_DETAILS =
            Set.of("PH", "TDS", "DHT", "FLOAT", "ARDUINO", "CAM", "CAM_DETAIL");
    private final DeviceRepository deviceRepository;

    @PostMapping("/api/device-selftests")
    public ResponseEntity<Void> receive(@Valid @RequestBody SelfTestReportRequestDto report) {
        try {
            OffsetDateTime.parse(report.timestamp());
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().build();
        }
        if (!report.detail().keySet().containsAll(REQUIRED_DETAILS)
                || (report.status().equals("OK") && (!report.failed().isEmpty()
                        || !"OK".equals(report.detail().get("ARDUINO"))
                        || Set.of("PH", "TDS", "DHT", "FLOAT").stream()
                                .anyMatch(key -> !"OK".equals(report.detail().get(key)))
                        || "FAIL".equals(report.detail().get("CAM"))))
                || (report.status().equals("FAIL") && report.failed().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }
        if (!deviceRepository.existsById(report.serialNumber())) {
            return ResponseEntity.badRequest().build();
        }

        // One searchable line per run. The log is the record; no table is added.
        log.info("GROWLAB_SELFTEST serial={} requestId={} trigger={} deviceTimestamp={} status={} failed={} detail={}",
                report.serialNumber(), report.requestId(), report.trigger(), report.timestamp(),
                report.status(), oneLine(report.failed()), oneLine(report.detail()));
        return ResponseEntity.ok().build();
    }

    private static String oneLine(Object value) {
        return String.valueOf(value).replace('\r', ' ').replace('\n', ' ');
    }
}
