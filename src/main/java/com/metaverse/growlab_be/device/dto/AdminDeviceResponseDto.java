package com.metaverse.growlab_be.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.device.domain.Device;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminDeviceResponseDto {
    private String serialNumber;
    private String deviceNickname;
    private Boolean registered;      // 사용자에게 배정되었는지 여부
    private String ownerUsername;    // 배정된 경우 소유자 아이디, 미배정이면 null

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public AdminDeviceResponseDto(Device device) {
        this.serialNumber = device.getId();
        this.deviceNickname = device.getDeviceNickname();
        this.registered = device.getUser() != null;
        this.ownerUsername = device.getUser() != null ? device.getUser().getUsername() : null;
        this.createdAt = device.getCreatedAt();
    }
}
