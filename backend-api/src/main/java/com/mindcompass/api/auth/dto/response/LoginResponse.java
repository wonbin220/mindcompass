package com.mindcompass.api.auth.dto.response;

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.security.JwtTokenProvider;
import java.time.LocalDateTime;

// 로그인 성공 시 토큰과 사용자 요약 정보를 내려주는 응답 DTO입니다.
public record LoginResponse(
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt,
        UserSummary user
) {

    public static LoginResponse of(
            User user,
            JwtTokenProvider.GeneratedToken accessToken,
            JwtTokenProvider.GeneratedToken refreshToken
    ) {
        return new LoginResponse(
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt(),
                new UserSummary(user.getId(), user.getNickname())
        );
    }

    // 로그인 응답 안에 포함되는 사용자 요약 정보입니다.
    public record UserSummary(
            Long userId,
            String nickname
    ) {
    }
}
