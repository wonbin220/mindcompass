// 일기 감정 분석 기본 응답 문구를 검증하는 테스트입니다.
package com.mindcompass.aiapi.service;

import com.mindcompass.aiapi.dto.AnalyzeDiaryRequest;
import com.mindcompass.aiapi.dto.AnalyzeDiaryResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryAnalysisServiceTest {

    private final DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService();

    @Test
    void analyzeReturnsUtf8KoreanSummaryForEmptyContent() {
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "   ", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("CALM");
        assertThat(response.summary()).isEqualTo("내용이 비어 있어 기본 감정 결과로 반환합니다.");
    }

    @Test
    void analyzeReturnsUtf8KoreanSummaryForAnxiousContent() {
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "오늘은 불안하고 걱정이 많아서 초조했다.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("ANXIOUS");
        assertThat(response.summary()).isEqualTo("불안과 긴장감이 반복되어 드러나는 일기입니다.");
    }

    @Test
    void analyzeReturnsUtf8KoreanSummaryForDefaultContent() {
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "산책도 하고 밥도 먹고 기록을 남겼다.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("CALM");
        assertThat(response.summary()).isEqualTo("Spring AI 비교용 기본 감정 분석 결과입니다.");
    }
}