package com.mindcompass.api.report.dto.response;

// 위험도 그래프의 날짜별 포인트를 내려주는 응답 DTO다.

import java.time.LocalDate;

public record RiskTrendPointResponse(
        LocalDate date,
        long mediumCount,
        long highCount
) {
}
