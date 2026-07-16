package com.metaverse.growlab_be.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.growlab_be.auth.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminUserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public AdminUserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getUserRole().name();
        this.createdAt = user.getCreatedAt();
    }
}
