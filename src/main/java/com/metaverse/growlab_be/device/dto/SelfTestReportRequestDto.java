package com.metaverse.growlab_be.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record SelfTestReportRequestDto(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,64}") String serialNumber,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{32}") String requestId,
        @NotBlank @Pattern(regexp = "scheduled|manual") String trigger,
        @NotBlank String timestamp,
        @NotBlank @Pattern(regexp = "OK|FAIL") String status,
        @NotEmpty @Size(max = 16) Map<String, String> detail,
        @NotNull @Size(max = 16) List<String> failed
) {
}
