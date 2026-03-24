// 상담 답변 생성 응답 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import java.math.BigDecimal;

public record GenerateReplyResponse(
        String reply,
        BigDecimal confidence,
        String responseType
) {
}
