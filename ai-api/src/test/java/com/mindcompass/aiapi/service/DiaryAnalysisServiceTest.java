// ?쇨린 媛먯젙 遺꾩꽍???꾨＼?꾪듃 ?깃났怨?fallback ?숈옉???④퍡 寃利앺븯???뚯뒪?몃떎.
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
                          \"summary\": \"怨쇰??섏? 遺덉븞???④퍡 ?댁뼱吏??섎（??湲곕줉?낅땲??\",
                          \"confidence\": 0.88
                        }
                        """,
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "?ㅻ뒛? ?덈Т 踰꾧쾪怨?遺덉븞?덈떎.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("OVERWHELMED");
        assertThat(response.emotionIntensity()).isEqualTo(5);
        assertThat(response.emotionTags()).containsExactly("OVERWHELMED", "ANXIOUS");
        assertThat(response.summary()).isEqualTo("怨쇰??섏? 遺덉븞???④퍡 ?댁뼱吏??섎（??湲곕줉?낅땲??");
        assertThat(response.confidence()).isEqualByComparingTo("0.88");
    }

    @Test
    void analyzeFallsBackToRuleBasedResultWhenPromptResponseIsInvalid() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> "not-json-response",
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "?ㅻ뒛? 遺덉븞?섍퀬 嫄깆젙??留롮븘??珥덉“?덈떎.", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("ANXIOUS");
        assertThat(response.summary()).isEqualTo("遺덉븞怨?湲댁옣??諛섎났?섍퀬 ?쇱긽 吏묒쨷???대젮?좊뜕 湲곕줉?쇰줈 ?댁꽍?⑸땲??");
        assertThat(response.confidence()).isEqualByComparingTo("0.73");
    }

    @Test
    void analyzeFallsBackWhenPromptClientIsUnavailable() {
        DiaryAnalysisService diaryAnalysisService = new DiaryAnalysisService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(
                new AnalyzeDiaryRequest(1L, 1L, "?곗콉?섍퀬 諛λ룄 癒밴퀬 湲곕줉???④꼈??", "2026-03-24T21:30:00")
        );

        assertThat(response.primaryEmotion()).isEqualTo("CALM");
        assertThat(response.summary()).isEqualTo("媛먯젙 ?먮쫫怨??쇱긽 留λ씫???④퍡 ?댄렣蹂??꾩슂媛 ?덈뒗 湲곕줉?낅땲??");
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
        assertThat(response.summary()).isEqualTo("?댁슜??鍮꾩뼱 ?덉뼱 湲곕낯 媛먯젙 寃곌낵濡?諛섑솚?⑸땲??");
        assertThat(response.confidence()).isEqualByComparingTo(BigDecimal.valueOf(0.10).setScale(2));
    }
}
