// 일기 내용을 비교용 기본 감정 분석 결과로 정리하는 서비스입니다.
package com.mindcompass.aiapi.service;

import com.mindcompass.aiapi.dto.AnalyzeDiaryRequest;
import com.mindcompass.aiapi.dto.AnalyzeDiaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiaryAnalysisService {

    private static final String EMPTY_CONTENT_SUMMARY = "내용이 비어 있어 기본 감정 결과로 반환합니다.";
    private static final String ANXIOUS_SUMMARY = "불안과 긴장감이 반복되어 드러나는 일기입니다.";
    private static final String DEFAULT_SUMMARY = "Spring AI 비교용 기본 감정 분석 결과입니다.";

    public AnalyzeDiaryResponse analyze(AnalyzeDiaryRequest request) {
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            return new AnalyzeDiaryResponse(
                    "CALM",
                    1,
                    List.of("CALM"),
                    EMPTY_CONTENT_SUMMARY,
                    BigDecimal.valueOf(0.10)
            );
        }

        if (containsAny(content, "불안", "걱정", "초조")) {
            return new AnalyzeDiaryResponse(
                    "ANXIOUS",
                    4,
                    List.of("ANXIOUS", "TENSE"),
                    ANXIOUS_SUMMARY,
                    BigDecimal.valueOf(0.73)
            );
        }

        return new AnalyzeDiaryResponse(
                "CALM",
                2,
                List.of("CALM"),
                DEFAULT_SUMMARY,
                BigDecimal.valueOf(0.50)
        );
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}