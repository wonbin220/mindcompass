package com.mindcompass.api.report.dto.response;

// 최근 7일 감정 추이를 내려주는 응답 DTO다.

import java.time.LocalDate;
import java.util.List;

public record WeeklyEmotionTrendResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<EmotionTrendPointResponse> items
) {
}
