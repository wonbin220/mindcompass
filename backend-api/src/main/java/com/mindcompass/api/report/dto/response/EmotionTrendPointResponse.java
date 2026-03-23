package com.mindcompass.api.report.dto.response;

// 감정 추이 그래프의 날짜별 포인트를 내려주는 응답 DTO다.

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import java.time.LocalDate;

public record EmotionTrendPointResponse(
        LocalDate date,
        boolean hasDiary,
        int diaryCount,
        PrimaryEmotion primaryEmotion,
        Integer averageEmotionIntensity
) {

    public static EmotionTrendPointResponse empty(LocalDate date) {
        return new EmotionTrendPointResponse(date, false, 0, null, null);
    }
}
