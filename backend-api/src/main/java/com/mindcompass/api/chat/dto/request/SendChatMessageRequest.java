package com.mindcompass.api.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 사용자가 채팅방에 보내는 메시지 요청을 검증하는 DTO입니다.
public record SendChatMessageRequest(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 2000, message = "메시지는 2000자 이하여야 합니다.")
        String message
) {
}
