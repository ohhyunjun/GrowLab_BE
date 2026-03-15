package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.dto.SignUpRequestDto;
import com.metaverse.growlab_be.auth.service.UserService;
import com.metaverse.growlab_be.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        try {
            userService.registerUser(signUpRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원 가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
