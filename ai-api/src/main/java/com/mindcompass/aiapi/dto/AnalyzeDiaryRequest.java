// 일기 감정 분석 요청 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalyzeDiaryRequest(
        @NotNull Long userId,
        @NotNull Long diaryId,
        @NotBlank String content,
        @NotBlank String writtenAt
) {
}
