// 상담 답변 생성 요청 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GenerateReplyRequest(
        @NotNull Long userId,
        @NotNull Long sessionId,
        @NotBlank String message,
        @Valid @NotEmpty List<ConversationTurn> conversationHistory,
        String memorySummary,
        String mode
) {
}
