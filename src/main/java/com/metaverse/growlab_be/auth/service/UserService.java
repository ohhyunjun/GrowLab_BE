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
}
