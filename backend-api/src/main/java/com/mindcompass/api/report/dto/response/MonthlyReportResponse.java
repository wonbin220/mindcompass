package com.mindcompass.api.report.dto.response;

// 월간 리포트 화면에 필요한 통계 요약을 내려주는 응답 DTO다.

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        long diaryCount,
        BigDecimal averageEmotionIntensity,
        List<EmotionCountResponse> topPrimaryEmotions,
        RiskSummaryResponse riskSummary
) {
}
