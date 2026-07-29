package com.metaverse.growlab_be.auth.repository;

import com.metaverse.growlab_be.auth.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);

    // 만료됐는데 끝내 인증 안 된 레코드 정리용
    List<EmailVerification> findAllByExpiresAtBeforeAndVerifiedFalse(LocalDateTime cutoff);
}
