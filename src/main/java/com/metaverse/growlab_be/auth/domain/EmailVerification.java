package com.metaverse.growlab_be.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "email_verification")
public class EmailVerification {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    // ✅ 인증 시도(실패) 횟수 - MAX_ATTEMPTS 넘으면 더 이상 시도 불가, 재발송 필요
    @Column(nullable = false)
    private int attemptCount = 0;

    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
    }

    // 같은 이메일로 재발송 시 코드/만료시간 갱신 + 시도 횟수 초기화
    public void renew(String code, LocalDateTime expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
    }

    public void markVerified() {
        this.verified = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    // ✅ 시도 횟수 소진 여부
    public boolean isAttemptsExceeded() {
        return this.attemptCount >= MAX_ATTEMPTS;
    }

    // ✅ 실패 시 시도 횟수 증가
    public void increaseAttempt() {
        this.attemptCount++;
    }
}