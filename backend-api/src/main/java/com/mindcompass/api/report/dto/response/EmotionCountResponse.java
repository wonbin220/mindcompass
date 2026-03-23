package com.mindcompass.api.report.dto.response;

// 월간 리포트에서 감정별 빈도를 내려주는 응답 DTO다.

import com.mindcompass.api.diary.domain.PrimaryEmotion;

public record EmotionCountResponse(
        PrimaryEmotion emotion,
        long count
) {
}
