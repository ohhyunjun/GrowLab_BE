package com.metaverse.growlab_be.device.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.dto.DeviceCreateRequestDto;
import com.metaverse.growlab_be.device.dto.DeviceRegistrationRequestDto;
import com.metaverse.growlab_be.device.dto.DeviceResponseDto;
import com.metaverse.growlab_be.device.dto.PhotoIntervalRequestDto;
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
        List<DeviceResponseDto> devices = deviceService.getUserDevices(currentUser);
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerDevice(
            @Valid @RequestBody DeviceCreateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        try {
            User currentUser = principalDetails.getUser();

            deviceService.registerDevice(requestDto.getSerialNumber(), requestDto.getDeviceNickname(),currentUser);

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
            User currentUser = principalDetails.getUser();
            deviceService.deleteDevice(serialNumber, currentUser);
            return ResponseEntity.ok("기기가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    //사진 촬영 주기 설정
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
            return ResponseEntity.ok(
                    requestDto.getPhotoInterval() + "시간마다 촬영으로 설정되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
