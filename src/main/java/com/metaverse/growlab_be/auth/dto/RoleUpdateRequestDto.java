package com.metaverse.growlab_be.auth.dto;

import com.metaverse.growlab_be.auth.domain.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoleUpdateRequestDto {

    @NotNull(message = "변경할 권한은 필수입니다.")
    private UserRole role;
}
