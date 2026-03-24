// 위험 신호 스코어링 응답 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import java.math.BigDecimal;
import java.util.List;

public record RiskScoreResponse(
        String riskLevel,
        BigDecimal riskScore,
        List<String> signals,
        String recommendedAction
) {
}
