package com.metaverse.growlab_be.auth.controller;

import com.metaverse.growlab_be.auth.dto.EmailCodeRequestDto;
import com.metaverse.growlab_be.auth.service.EmailVerificationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class EmailDeliveryErrorTest {
    private static final String EMAIL = "test@example.com";

    @ParameterizedTest
    @ValueSource(strings = {"signup", "password-reset", "find-id"})
    void smtpConnectionFailureIsUnavailable(String flow) {
        var response = send(flow, new MailSendException("private SMTP hostname"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo("메일 발송 서버 오류입니다. 관리자에게 문의해주세요.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"signup", "password-reset", "find-id"})
    void smtpAuthenticationFailureIsUnavailable(String flow) {
        var response = send(flow, new MailAuthenticationException("private SMTP account"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).doesNotContain("private SMTP account");
    }

    @ParameterizedTest
    @ValueSource(strings = {"signup", "password-reset", "find-id"})
    void databaseFailureRemainsInternalError(String flow) {
        var response = send(flow, new DataAccessResourceFailureException("private database hostname"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).doesNotContain("private database hostname");
    }

    @ParameterizedTest
    @ValueSource(strings = {"signup", "password-reset", "find-id"})
    void userValidationErrorRemainsBadRequest(String flow) {
        var response = send(flow, new IllegalArgumentException("이메일을 확인해주세요."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("이메일을 확인해주세요.");
    }

    private ResponseEntity<String> send(String flow, RuntimeException failure) {
        EmailVerificationService service = mock(EmailVerificationService.class);
        EmailCodeRequestDto request = new EmailCodeRequestDto();
        ReflectionTestUtils.setField(request, "email", EMAIL);
        return switch (flow) {
            case "signup" -> {
                doThrow(failure).when(service).sendVerificationCode(EMAIL);
                yield new EmailVerificationController(service).sendCode(request);
            }
            case "password-reset" -> {
                doThrow(failure).when(service).sendPasswordResetCode(EMAIL);
                yield new PasswordResetController(service, null).sendCode(request);
            }
            case "find-id" -> {
                doThrow(failure).when(service).sendFindIdCode(EMAIL);
                yield new FindIdController(service, null).sendCode(request);
            }
            default -> throw new IllegalArgumentException(flow);
        };
    }
}
