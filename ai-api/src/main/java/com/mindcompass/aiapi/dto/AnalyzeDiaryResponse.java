// 일기 감정 분석 응답 계약을 담는 DTO입니다.
package com.mindcompass.aiapi.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyzeDiaryResponse(
        String primaryEmotion,
        Integer emotionIntensity,
        List<String> emotionTags,
        String summary,
        BigDecimal confidence
) {
}
