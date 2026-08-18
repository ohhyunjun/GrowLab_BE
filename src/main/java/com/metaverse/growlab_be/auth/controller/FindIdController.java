package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.dto.EmailCodeRequestDto;
import com.metaverse.growlab_be.auth.dto.FindIdRequestDto;
import com.metaverse.growlab_be.auth.dto.FindIdResponseDto;
import com.metaverse.growlab_be.auth.service.EmailVerificationService;
import com.metaverse.growlab_be.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth/find-id")
@RequiredArgsConstructor
public class FindIdController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    // 1단계: 인증코드 발송 (가입된 이메일만 가능)
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@Valid @RequestBody EmailCodeRequestDto requestDto) {
        try {
            emailVerificationService.sendFindIdCode(requestDto.getEmail());
            return ResponseEntity.ok("인증코드가 발송되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 2단계: 인증 완료된 이메일로 마스킹된 아이디 조회
    // (코드 검증 자체는 /api/auth/email/verify-code 재사용)
    @PostMapping
    public ResponseEntity<?> findId(@Valid @RequestBody FindIdRequestDto requestDto) {
        try {
            String masked = userService.findUsername(requestDto.getEmail());
            return ResponseEntity.ok(new FindIdResponseDto(masked));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
