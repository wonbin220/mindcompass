package com.mindcompass.api.auth.dto.response;

import com.mindcompass.api.auth.domain.User;
import java.time.LocalDateTime;

// 회원가입 완료 후 생성된 사용자 정보를 내려주는 응답 DTO입니다.
public record SignUpResponse(
        Long userId,
        String email,
        String nickname,
        LocalDateTime createdAt
) {

    public static SignUpResponse from(User user) {
        return new SignUpResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt()
        );
    }
}
