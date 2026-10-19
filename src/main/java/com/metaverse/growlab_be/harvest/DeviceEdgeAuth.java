package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Per-device credentials. Unconfigured devices cannot write edge predictions. */
@Component
public class DeviceEdgeAuth {
    private final Map<String, String> keys;
    public DeviceEdgeAuth(@Value("${growlab.edge.device-keys-json:${GROWLAB_EDGE_DEVICE_KEYS_JSON:{}}}") String json) {
        try { keys = new ObjectMapper().readValue(json, new TypeReference<Map<String,String>>() {}); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid edge device key configuration"); }
    }
    public boolean isConfigured(String serial) { return serial != null && keys.containsKey(serial); }
    public void verify(String serial, String provided) {
        String expected = keys.get(serial);
        if (expected == null || expected.length() < 32 || provided == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "기기 인증이 필요합니다.");
        }
    }
}
