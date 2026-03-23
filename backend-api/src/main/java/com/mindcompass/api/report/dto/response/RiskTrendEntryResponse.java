package com.mindcompass.api.report.dto.response;

// 위험도 추이 집계를 위한 원본 조회 결과 DTO다.

import java.time.LocalDateTime;

public record RiskTrendEntryResponse(
        String riskLevel,
        LocalDateTime writtenAt
) {
}
