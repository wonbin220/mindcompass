package com.mindcompass.api.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 상담 세션 생성 요청 본문을 검증하는 DTO입니다.
public record CreateChatSessionRequest(
        @NotBlank(message = "세션 제목은 필수입니다.")
        @Size(max = 100, message = "세션 제목은 100자 이하여야 합니다.")
        String title,
        Long sourceDiaryId
) {
}
