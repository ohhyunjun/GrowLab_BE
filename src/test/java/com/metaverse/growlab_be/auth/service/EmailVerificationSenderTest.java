package com.metaverse.growlab_be.auth.service;

import com.metaverse.growlab_be.auth.domain.EmailVerification;
import com.metaverse.growlab_be.auth.repository.EmailVerificationRepository;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationSenderTest {
    @ParameterizedTest
    @CsvSource({"signup,false", "password-reset,false", "find-id,false",
            "signup,true", "password-reset,true", "find-id,true"})
    void allCodeEmailsHaveAnExplicitSenderWithoutLocalAddressLookup(String flow, boolean overrideFrom)
            throws Exception {
        String recipient = "recipient@example.com";
        String username = "smtp-user@example.com";
        String expectedFrom = overrideFrom ? "verified-sender@example.com" : username;
        var verifications = mock(EmailVerificationRepository.class);
        var users = mock(UserRepository.class);
        var mailSender = mock(JavaMailSender.class);
        when(users.existsByEmail(recipient)).thenReturn(!flow.equals("signup"));
        when(verifications.findByEmail(recipient)).thenReturn(Optional.empty());

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.mail.username", username);
        if (overrideFrom) {
            properties.put("spring.mail.from", expectedFrom);
        }
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("smtp", properties));
            context.registerBean(EmailVerificationService.class,
                    () -> new EmailVerificationService(verifications, users, mailSender));
            context.refresh();
            var service = context.getBean(EmailVerificationService.class);
            switch (flow) {
                case "signup" -> service.sendVerificationCode(recipient);
                case "password-reset" -> service.sendPasswordResetCode(recipient);
                case "find-id" -> service.sendFindIdCode(recipient);
                default -> throw new IllegalArgumentException(flow);
            }
        }

        var email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertThat(email.getValue().getFrom()).isEqualTo(expectedFrom);
        assertThat(email.getValue().getTo()).containsExactly(recipient);

        var savedCode = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verifications).save(savedCode.capture());
        assertThat(email.getValue().getText()).contains(savedCode.getValue().getCode());

        // Serialize the real MIME message without an SMTP connection or OS mail address.
        var mime = new MimeMessage(Session.getInstance(new Properties()));
        email.getValue().copyTo(new MimeMailMessage(mime));
        mime.saveChanges();
        assertThat(mime.getFrom()).hasSize(1);
        assertThat(((InternetAddress) mime.getFrom()[0]).getAddress()).isEqualTo(expectedFrom);
    }
}
