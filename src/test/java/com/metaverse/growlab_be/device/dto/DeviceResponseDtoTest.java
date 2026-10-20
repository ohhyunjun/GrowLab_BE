package com.metaverse.growlab_be.device.dto;

import com.metaverse.growlab_be.device.domain.Device;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceResponseDtoTest {

    @Test
    void omitsDeviceWideSpeciesFields() {
        Device device = new Device("GROWLAB-G111", "test");
        DeviceResponseDto response = new DeviceResponseDto(device, null, List.of());
        JsonNode json = new ObjectMapper().valueToTree(response);

        assertThat(json.has("speciesId")).isFalse();
        assertThat(json.has("speciesName")).isFalse();
        assertThat(json.has("minTemperature")).isFalse();
        assertThat(json.has("stageNames")).isFalse();
        assertThat(json.has("plants")).isTrue();
    }
}
