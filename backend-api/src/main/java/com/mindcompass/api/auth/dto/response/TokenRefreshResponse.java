package com.mindcompass.api.auth.dto.response;

import java.time.LocalDateTime;

// refresh token 재발급 결과를 내려주는 응답 DTO입니다.
public record TokenRefreshResponse(
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}
