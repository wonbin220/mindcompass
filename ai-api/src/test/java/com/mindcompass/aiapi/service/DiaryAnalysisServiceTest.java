// 일기 감정 분석은 프롬프트 성공과 fallback 동작을 함께 검증하는 테스트입니다.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.AnalyzeDiaryRequest;
import com.mindcompass.aiapi.dto.AnalyzeDiaryResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryAnalysisServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzeReturnsPromptResultWhenStructuredJsonIsValid() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> """
                        {
                          \"primaryEmotion\": \"OVERWHELMED\",
                          \"emotionIntensity\": 5,
                          \"emotionTags\": [\"OVERWHELMED\", \"ANXIOUS\"],
                          \"summary\": \"과부하와 불안이 함께 드러나는 기록입니다.\",
                          \"confidence\": 0.88
                        }
                        """,
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "오늘은 너무 벅차고 불안했다.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("OVERWHELMED");
        assertThat(response.emotionIntensity()).isEqualTo(5);
        assertThat(response.emotionTags()).containsExactly("OVERWHELMED", "ANXIOUS");
        assertThat(response.summary()).isEqualTo("과부하와 불안이 함께 드러나는 기록입니다.");
        assertThat(response.confidence()).isEqualByComparingTo("0.88");
    }

    @Test
    void analyzeFallsBackToRuleBasedResultWhenPromptResponseIsInvalid() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> "not-json-response",
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "오늘은 불안하고 걱정이 많아서 초조했다.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("ANXIOUS");
        assertThat(response.summary()).isEqualTo("불안과 긴장감이 반복되어 드러나는 일기입니다.");
        assertThat(response.confidence()).isEqualByComparingTo("0.73");
    }

    @Test
    void analyzeFallsBackWhenPromptClientIsUnavailable() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "산책도 하고 밥도 먹고 기록을 남겼다.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("CALM");
        assertThat(response.summary()).isEqualTo("Spring AI 비교용 기본 감정 분석 결과입니다.");
        assertThat(response.confidence()).isEqualByComparingTo("0.50");
    }

    @Test
    void analyzeReturnsUtf8KoreanSummaryForEmptyContent() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "   ", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("CALM");
        assertThat(response.summary()).isEqualTo("내용이 비어 있어 기본 감정 결과로 반환합니다.");
        assertThat(response.confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.10).setScale(2));
    }
}