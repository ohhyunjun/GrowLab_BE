package com.metaverse.growlab_be.auth.service;

import com.metaverse.growlab_be.auth.domain.EmailVerification;
import com.metaverse.growlab_be.auth.repository.EmailVerificationRepository;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private static final long EXPIRATION_MINUTES = 5;
    private final SecureRandom random = new SecureRandom();

    // 인증코드 발송 (재발송 시 기존 코드 갱신)
    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

        emailVerificationRepository.findByEmail(email)
                .ifPresentOrElse(
                        v -> v.renew(code, expiresAt),
                        () -> emailVerificationRepository.save(new EmailVerification(email, code, expiresAt))
                );

        sendMail(email, code);
    }

    // 코드 검증
    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다. 인증코드를 먼저 받아주세요."));

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다. 다시 받아주세요.");
        }

        if (!verification.getCode().equals(code)) {
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }

        verification.markVerified();
    }

    // 회원가입 시 이 이메일이 인증 완료 상태인지 확인
    @Transactional(readOnly = true)
    public boolean isVerified(String email) {
        return emailVerificationRepository.findByEmail(email)
                .map(EmailVerification::isVerified)
                .orElse(false);
    }

    // 회원가입 성공 후 인증 레코드 정리
    @Transactional
    public void deleteVerification(String email) {
        emailVerificationRepository.findByEmail(email)
                .ifPresent(emailVerificationRepository::delete);
    }

    // 매시 정각에 만료됐는데 끝내 인증 안 된 레코드 정리
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpired() {
        emailVerificationRepository
                .findAllByExpiresAtBeforeAndVerifiedFalse(LocalDateTime.now())
                .forEach(emailVerificationRepository::delete);
    }

    private String generateCode() {
        int number = random.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private void sendMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[GrowLab] 이메일 인증 코드");
        message.setText("인증코드는 " + code + " 입니다. " + EXPIRATION_MINUTES + "분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}