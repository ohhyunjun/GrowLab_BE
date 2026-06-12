package com.metaverse.growlab_be.device.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.dto.*;
import com.metaverse.growlab_be.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<DeviceResponseDto>> getUserDevices(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        return ResponseEntity.ok(deviceService.getUserDevices(currentUser));
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerDevice(
            @Valid @RequestBody DeviceCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            deviceService.registerDevice(
                    requestDto.getSerialNumber(),
                    requestDto.getDeviceNickname(),
                    principalDetails.getUser());
            return ResponseEntity.ok("기기가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createDevice(@RequestBody DeviceRegistrationRequestDto requestDto) {
        try {
            deviceService.createDeviceByAdmin(requestDto.getSerialNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body("기기가 성공적으로 생성되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<String> deleteDevice(
            @PathVariable String serialNumber,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            deviceService.deleteDevice(serialNumber, principalDetails.getUser());
            return ResponseEntity.ok("기기가 성공적으로 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/{serialNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDeviceByAdmin(@PathVariable String serialNumber) {
        try {
            deviceService.deleteDeviceByAdmin(serialNumber);
            return ResponseEntity.ok("기기가 완전히 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{serialNumber}/photo_interval")
    public ResponseEntity<String> updatePhotoInterval(
            @PathVariable String serialNumber,
            @Valid @RequestBody PhotoIntervalRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            deviceService.updatePhotoInterval(
                    serialNumber,
                    requestDto.getPhotoInterval(),
                    principalDetails.getUser());
            return ResponseEntity.ok(requestDto.getPhotoInterval() + "시간마다 촬영으로 설정되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PatchMapping("/{serialNumber}/led")
    public ResponseEntity<String> updateLed(
            @PathVariable String serialNumber,
            @RequestBody LedRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            deviceService.controlLed(serialNumber, requestDto, principalDetails.getUser());
            String msg = Boolean.TRUE.equals(requestDto.getLedMode())
                    ? "LED 스케줄이 설정되었습니다."
                    : Boolean.TRUE.equals(requestDto.getLedStatus()) ? "LED가 켜졌습니다." : "LED가 꺼졌습니다.";
            return ResponseEntity.ok(msg);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PatchMapping("/{serialNumber}/ports")
    public ResponseEntity<String> updatePortStatus(
            @PathVariable String serialNumber,
            @RequestBody PortStatusRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            deviceService.updatePortStatus(
                    serialNumber,
                    requestDto.getPortIndex(),
                    requestDto.getStatus(),
                    principalDetails.getUser());
            return ResponseEntity.ok(requestDto.getPortIndex() + "번 포트 상태 변경 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ✅ 기기 대표 품종 설정/변경 - PATCH /api/devices/{serialNumber}/species
    @PatchMapping("/{serialNumber}/species")
    public ResponseEntity<?> updateDeviceSpecies(
            @PathVariable String serialNumber,
            @Valid @RequestBody DeviceSpeciesRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            DeviceResponseDto result = deviceService.updateDeviceSpecies(
                    serialNumber,
                    requestDto.getSpeciesId(),
                    principalDetails.getUser());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
