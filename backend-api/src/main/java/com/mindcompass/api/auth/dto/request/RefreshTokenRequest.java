package com.mindcompass.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

// access token 재발급에 필요한 refresh token 요청 DTO입니다.
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
