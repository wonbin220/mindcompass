package com.mindcompass.api.diary.dto.request;

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

// 일기 한 건에 연결할 개별 감정 태그 입력 DTO입니다.
public record EmotionTagRequest(
        @NotNull(message = "emotionCode is required")
        PrimaryEmotion emotionCode,

        @Max(value = 5, message = "intensity must be 5 or less")
        Integer intensity
) {
}
