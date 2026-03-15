package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.dto.AuthResponseDto;
import com.metaverse.growlab_be.auth.dto.LoginRequestDto;
import com.metaverse.growlab_be.auth.dto.SignUpRequestDto;
import com.metaverse.growlab_be.auth.service.UserService;
import com.metaverse.growlab_be.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("login")
    public ResponseEntity<AuthResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        try {
            // 인증 객체 생성
            // Authentication Manager를 통해서 username, password 기반으로 인증 수행 지시
            // loadUserByUsername과 passwordEncoder.matches()가 내부적으로 실행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getUsername(),
                            loginRequestDto.getPassword()
                    )
            );
            // 결과물 인증 객체 (UserDetails == Principal)
            // @AuthenticationPrincipal 애너테이션이 내부적으로 (UserDetails) 형변환을 해줌
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // JWT 토큰 생성 (accessToken = jwtToken)
            String accessToken = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(new AuthResponseDto(userDetails.getUsername(), accessToken));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDto(null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponseDto(null, null));
        }
    }
}
