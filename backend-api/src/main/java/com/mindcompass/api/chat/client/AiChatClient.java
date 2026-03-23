package com.mindcompass.api.chat.client;

import com.mindcompass.api.auth.domain.ResponseMode;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
// Spring Boot에서 ai-api의 generate-reply 내부 API를 호출하는 클라이언트입니다.
public class AiChatClient {

    private final WebClient aiWebClient;

    public AiChatClient(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public GenerateReplyResponse generateReply(GenerateReplyRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/generate-reply")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenerateReplyResponse.class)
                .block();
    }

    // ai-api generate-reply 요청 계약입니다.
    public record GenerateReplyRequest(
            Long userId,
            Long sessionId,
            String message,
            List<ConversationTurn> conversationHistory,
            String memorySummary,
            ResponseMode mode
    ) {
    }

    // ai-api에 전달하는 최근 대화 한 턴입니다.
    public record ConversationTurn(
            String role,
            String content
    ) {
    }

    // ai-api generate-reply 응답 계약입니다.
    public record GenerateReplyResponse(
            String reply,
            BigDecimal confidence,
            String responseType
    ) {
    }
}
