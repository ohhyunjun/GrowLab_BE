package com.metaverse.growlab_be.auth.service;

import com.metaverse.growlab_be.auth.controller.UserController;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceAdminDeleteTest {
    @Test
    void registeredDeviceBlocksAdminDeletionWithActionableMessage() {
        User admin = new User();
        admin.setId(1L);
        User target = new User();
        target.setId(2L);
        target.getDevices().add(new Device("GROWLAB-TEST", "테스트 기기"));
        UserRepository users = mock(UserRepository.class);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        UserService service = new UserService(users, mock(PasswordEncoder.class),
                mock(EmailVerificationService.class));

        assertThatThrownBy(() -> service.deleteUserByAdmin(2L, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용자의 기기를 제거한 후 강퇴할 수 있습니다.");
        verify(users, never()).delete(any(User.class));

        var response = new UserController(service).deleteUserByAdmin(2L, new PrincipalDetails(admin));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("사용자의 기기를 제거한 후 강퇴할 수 있습니다.");
    }

    @Test
    void adminCanDeleteUserWithoutDevice() {
        User admin = new User();
        admin.setId(1L);
        User target = new User();
        target.setId(2L);
        UserRepository users = mock(UserRepository.class);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        UserService service = new UserService(users, mock(PasswordEncoder.class),
                mock(EmailVerificationService.class));

        service.deleteUserByAdmin(2L, admin);

        verify(users).delete(target);
    }
}
