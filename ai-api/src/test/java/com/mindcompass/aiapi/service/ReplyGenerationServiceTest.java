// 상담 답변 프롬프트 성공과 fallback 동작을 함께 검증하는 테스트다.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.ConversationTurn;
import com.mindcompass.aiapi.dto.GenerateReplyRequest;
import com.mindcompass.aiapi.dto.GenerateReplyResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyGenerationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateReturnsPromptResultWhenStructuredJsonIsValid() {
        ReplyGenerationService replyGenerationService = new ReplyGenerationService(
                (systemPrompt, userPrompt) -> """
                        {
                          \"reply\": \"오늘 많이 버거우셨겠어요. 가장 크게 흔들린 순간을 하나만 같이 짚어볼까요?\",
                          \"confidence\": 0.84,
                          \"responseType\": \"NORMAL\"
                        }
                        """,
                objectMapper
        );

        GenerateReplyResponse response = replyGenerationService.generate(sampleRequest("오늘 너무 불안해서 아무것도 못 했어요."));

        assertThat(response.reply()).contains("가장 크게 흔들린 순간");
        assertThat(response.confidence()).isEqualByComparingTo("0.84");
        assertThat(response.responseType()).isEqualTo("NORMAL");
    }

    @Test
    void generateFallsBackWhenPromptResponseIsInvalid() {
        ReplyGenerationService replyGenerationService = new ReplyGenerationService(
                (systemPrompt, userPrompt) -> "not-json-response",
                objectMapper
        );

        GenerateReplyResponse response = replyGenerationService.generate(sampleRequest("오늘 너무 불안해서 아무것도 못 했어요."));

        assertThat(response.reply()).isNotBlank();
        assertThat(response.confidence()).isEqualByComparingTo("0.45");
        assertThat(response.responseType()).isEqualTo("FALLBACK");
    }

    @Test
    void generateFallsBackWhenPromptClientIsUnavailable() {
        ReplyGenerationService replyGenerationService = new ReplyGenerationService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        GenerateReplyResponse response = replyGenerationService.generate(sampleRequest("조금 지치지만 그래도 이야기해보고 싶어요."));

        assertThat(response.reply()).isNotBlank();
        assertThat(response.confidence()).isEqualByComparingTo("0.45");
        assertThat(response.responseType()).isEqualTo("FALLBACK");
    }

    @Test
    void generateReturnsFallbackForEmptyMessage() {
        ReplyGenerationService replyGenerationService = new ReplyGenerationService(
                (systemPrompt, userPrompt) -> null,
                objectMapper
        );

        GenerateReplyResponse response = replyGenerationService.generate(sampleRequest("   "));

        assertThat(response.reply()).isNotBlank();
        assertThat(response.confidence()).isEqualByComparingTo("0.15");
        assertThat(response.responseType()).isEqualTo("FALLBACK");
    }

    private GenerateReplyRequest sampleRequest(String message) {
        return new GenerateReplyRequest(
                1L,
                10L,
                message,
                List.of(
                        new ConversationTurn("user", "요즘 계속 마음이 불안해요."),
                        new ConversationTurn("assistant", "최근 어떤 상황에서 가장 불안함이 커지는지 이야기해볼까요?")
                ),
                "사용자는 업무 압박과 수면 부족을 자주 말함",
                "EMPATHETIC_WITH_EVIDENCE"
        );
    }
}
