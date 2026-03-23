package com.mindcompass.api.report.dto.response;

// 월간 리포트에서 위험도 집계를 내려주는 응답 DTO다.

public record RiskSummaryResponse(
        long mediumCount,
        long highCount
) {
}
