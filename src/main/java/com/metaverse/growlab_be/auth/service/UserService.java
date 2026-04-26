package com.metaverse.growlab_be.auth.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.domain.UserRole;
import com.metaverse.growlab_be.auth.dto.SignUpRequestDto;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(SignUpRequestDto signUpRequestDto) {
        if (userRepository.existsByUsername(signUpRequestDto.getUsername())) {
            throw new IllegalArgumentException("Username 사용자 계정이 사용중입니다.");
        }

        if (userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email 사용자 계정이 사용중입니다.");
        }

        User newUser = new User(
                signUpRequestDto.getUsername(),
                passwordEncoder.encode(signUpRequestDto.getPassword()),
                signUpRequestDto.getEmail(),
                UserRole.ROLE_USER // 사용자 역할 임시 하드코딩(추가로직 필요)
        );

        userRepository.save(newUser);
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

        //기존 비밀번호 검증
        if (!passwordEncoder.matches(oldPassword, findUser.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        //새 비밀번호 인코딩 후 저장
        findUser.setPassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void deleteUser(User user, String password) {
        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        userRepository.delete(user);
    }
}
