package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.dto.EmailCodeRequestDto;
import com.metaverse.growlab_be.auth.dto.EmailCodeVerifyRequestDto;
import com.metaverse.growlab_be.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@Valid @RequestBody EmailCodeRequestDto requestDto) {
        try {
            emailVerificationService.sendVerificationCode(requestDto.getEmail());
            return ResponseEntity.ok("인증코드가 발송되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody EmailCodeVerifyRequestDto requestDto) {
        try {
            emailVerificationService.verifyCode(requestDto.getEmail(), requestDto.getCode());
            return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
