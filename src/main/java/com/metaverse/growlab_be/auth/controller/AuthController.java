package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.dto.*;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getUsername(),
                            loginRequestDto.getPassword()
                    )
            );
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(principalDetails);

            return ResponseEntity.ok(new AuthResponseDto(
                    principalDetails.getUser().getId(),
                    principalDetails.getUsername(),
                    accessToken,
                    principalDetails.getUser().getUserRole().name()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponseDto(null, null, null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponseDto(null, null, null, null));
        }
    }

    @PutMapping("/username")
    public ResponseEntity<String> updateUsername(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody UpdateUsernameRequestDto dto
    ) {
        userService.updateUsername(principalDetails.getUser(), dto.getNewUsername());
        return ResponseEntity.ok("아이디 변경 완료");
    }

    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody UpdatePasswordRequestDto dto
    ) {
        userService.updatePassword(
                principalDetails.getUser(),
                dto.getOldPassword(),
                dto.getNewPassword()
        );
        return ResponseEntity.ok("비밀번호 변경 완료");
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<String> deleteUser(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam String password
    ) {
        userService.deleteUser(principalDetails.getUser(), password);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }
}