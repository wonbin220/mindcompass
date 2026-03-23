package com.mindcompass.api.report.dto.response;

// 월간 위험도 추이를 내려주는 응답 DTO다.

import java.util.List;

public record MonthlyRiskTrendResponse(
        int year,
        int month,
        List<RiskTrendPointResponse> items
) {
}
