package com.metaverse.growlab_be.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindIdResponseDto {
    // 보안을 위해 아이디 전체가 아닌 마스킹된 형태로 반환 (예: "hangabin" -> "ha****in")
    private String maskedUsername;
}
