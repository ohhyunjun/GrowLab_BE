package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.dto.AdminUserResponseDto;
import com.metaverse.growlab_be.auth.dto.RoleUpdateRequestDto;
import com.metaverse.growlab_be.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    // (관리자) 전체 회원 목록 조회 - GET /api/admin/users
    @GetMapping
    public ResponseEntity<List<AdminUserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // (관리자) 회원 강제 탈퇴 - DELETE /api/admin/users/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUserByAdmin(
            @PathVariable Long userId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            userService.deleteUserByAdmin(userId, principalDetails.getUser());
            return ResponseEntity.ok("회원이 강제 탈퇴되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // (관리자) 회원 권한 변경 - PATCH /api/admin/users/{userId}/role
    @PatchMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            AdminUserResponseDto result = userService.updateUserRole(
                    userId, requestDto.getRole(), principalDetails.getUser());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
