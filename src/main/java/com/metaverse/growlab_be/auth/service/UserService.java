package com.metaverse.growlab_be.auth.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.domain.UserRole;
import com.metaverse.growlab_be.auth.dto.AdminUserResponseDto;
import com.metaverse.growlab_be.auth.dto.SignUpRequestDto;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void registerUser(SignUpRequestDto signUpRequestDto) {
        if (userRepository.existsByUsername(signUpRequestDto.getUsername())) {
            throw new IllegalArgumentException("Username 사용자 계정이 사용중입니다.");
        }

        if (userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email 사용자 계정이 사용중입니다.");
        }

        if (!emailVerificationService.isVerified(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요.");
        }

        User newUser = new User(
                signUpRequestDto.getUsername(),
                passwordEncoder.encode(signUpRequestDto.getPassword()),
                signUpRequestDto.getEmail(),
                UserRole.ROLE_USER
        );

        userRepository.save(newUser);

        emailVerificationService.deleteVerification(signUpRequestDto.getEmail());
    }

    @Transactional
    public void updateUsername(User user, String newUsername) {
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("이미 사용 중인 username입니다.");
        }

        User findUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        findUser.setUsername(newUsername);
    }

    @Transactional
    public void updatePassword(User user, String oldPassword, String newPassword) {

        User findUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!passwordEncoder.matches(oldPassword, findUser.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        findUser.setPassword(passwordEncoder.encode(newPassword));
    }

    // ✅ 비밀번호 찾기(재설정) - 로그인 없이, 이메일 인증 완료 여부로만 검증
    @Transactional
    public void resetPassword(String email, String newPassword) {
        if (!emailVerificationService.isVerified(email)) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        user.setPassword(passwordEncoder.encode(newPassword));

        // 재설정 완료 후 인증 기록 정리 (재사용 방지)
        emailVerificationService.deleteVerification(email);
    }

    @Transactional
    public void deleteUser(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        userRepository.delete(user);
    }

    // ────────────────────────────────
    // ✅ 관리자 전용 메서드
    // ────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(AdminUserResponseDto::new)
                .toList();
    }

    @Transactional
    public void deleteUserByAdmin(Long targetUserId, User currentAdmin) {
        if (targetUserId.equals(currentAdmin.getId())) {
            throw new IllegalStateException("본인 계정은 이 기능으로 삭제할 수 없습니다.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + targetUserId));

        userRepository.delete(target);
    }

    @Transactional
    public AdminUserResponseDto updateUserRole(Long targetUserId, UserRole newRole, User currentAdmin) {
        if (targetUserId.equals(currentAdmin.getId())) {
            throw new IllegalStateException("본인 권한은 변경할 수 없습니다.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + targetUserId));

        target.setUserRole(newRole);
        return new AdminUserResponseDto(target);
    }
}