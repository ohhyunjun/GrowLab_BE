package com.metaverse.growlab_be.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordRequestDto {
    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;
}
