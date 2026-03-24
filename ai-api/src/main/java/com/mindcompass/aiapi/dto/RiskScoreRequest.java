// 위험 신호 스코어링 요청 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RiskScoreRequest(
        @NotNull Long userId,
        Long sessionId,
        @NotBlank String text,
        @NotBlank String sourceType
) {
}
