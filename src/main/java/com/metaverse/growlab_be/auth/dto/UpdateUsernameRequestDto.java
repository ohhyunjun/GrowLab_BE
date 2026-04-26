package com.metaverse.growlab_be.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUsernameRequestDto {
    @NotBlank
    private String newUsername;
}
