// 최근 대화 이력을 표현하는 DTO입니다.
package com.mindcompass.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationTurn(
        @NotBlank String role,
        @NotBlank String content
) {
}
