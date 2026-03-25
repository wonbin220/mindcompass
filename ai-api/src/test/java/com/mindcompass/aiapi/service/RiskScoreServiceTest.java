// 위험 점수 프롬프트 성공과 fallback 동작을 함께 검증하는 테스트다.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.RiskScoreRequest;
import com.mindcompass.aiapi.dto.RiskScoreResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void scoreReturnsPromptResultWhenStructuredJsonIsValid() {
        RiskScoreService riskScoreService = new RiskScoreService(
                (systemPrompt, userPrompt) -> """
                        {
                          \"riskLevel\": \"HIGH\",
                          \"riskScore\": 0.91,
                          \"signals\": [\"SELF_HARM_IMPLICIT\", \"HOPELESSNESS\"],
                          \"recommendedAction\": \"SAFETY_RESPONSE\"
                        }
                        """,
                objectMapper
        );

        RiskScoreResponse response = riskScoreService.score(
                new RiskScoreRequest(1L, 10L, "죽고 싶고 사라지고 싶어요.", "CHAT_MESSAGE")
        );

        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.riskScore()).isEqualByComparingTo("0.91");
        assertThat(response.signals()).containsExactly("SELF_HARM_IMPLICIT", "HOPELESSNESS");
        assertThat(response.recommendedAction()).isEqualTo("SAFETY_RESPONSE");
    }

    @Test
    void scoreFallsBackToRuleBasedResultWhenPromptResponseIsInvalid() {
        RiskScoreService riskScoreService = new RiskScoreService(
                (systemPrompt, userPrompt) -> "not-json-response",
                objectMapper
        );

        RiskScoreResponse response = riskScoreService.score(
                new RiskScoreRequest(1L, 10L, "아무도 없고 너무 힘들어서 버티기 힘들어요.", "CHAT_MESSAGE")
        );

        assertThat(response.riskLevel()).isEqualTo("MEDIUM");
        assertThat(response.riskScore()).isEqualByComparingTo("0.65");
        assertThat(response.signals()).contains("ISOLATION", "DISTRESS_ESCALATION");
        assertThat(response.recommendedAction()).isEqualTo("SUPPORTIVE_RESPONSE");
    }

    @Test
    void scoreFallsBackWhenPromptClientIsUnavailable() {
        RiskScoreService riskScoreService = new RiskScoreService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        RiskScoreResponse response = riskScoreService.score(
                new RiskScoreRequest(1L, 10L, "오늘은 조금 불안했지만 괜찮아질 것 같아요.", "CHAT_MESSAGE")
        );

        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.riskScore()).isEqualByComparingTo("0.10");
        assertThat(response.signals()).isEmpty();
        assertThat(response.recommendedAction()).isEqualTo("NORMAL_RESPONSE");
    }

    @Test
    void scoreReturnsLowRiskFallbackForEmptyText() {
        RiskScoreService riskScoreService = new RiskScoreService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        RiskScoreResponse response = riskScoreService.score(
                new RiskScoreRequest(1L, 10L, "   ", "CHAT_MESSAGE")
        );

        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.riskScore()).isEqualByComparingTo("0.05");
        assertThat(response.signals()).isEmpty();
        assertThat(response.recommendedAction()).isEqualTo("NORMAL_RESPONSE");
    }
}
