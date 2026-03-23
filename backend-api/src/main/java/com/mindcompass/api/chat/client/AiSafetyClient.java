package com.mindcompass.api.chat.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
// Spring Boot에서 ai-api risk-score 내부 API를 호출하는 안전 점수 클라이언트다.
public class AiSafetyClient {

    private final WebClient aiWebClient;

    public AiSafetyClient(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public RiskScoreResponse scoreRisk(RiskScoreRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/risk-score")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskScoreResponse.class)
                .block();
    }

    // ai-api risk-score 요청 계약이다.
    public record RiskScoreRequest(
            Long userId,
            Long sessionId,
            String text,
            String sourceType
    ) {
    }

    // ai-api risk-score 응답 계약이다.
    public record RiskScoreResponse(
            String riskLevel,
            BigDecimal riskScore,
            List<String> signals,
            String recommendedAction
    ) {
    }
}
