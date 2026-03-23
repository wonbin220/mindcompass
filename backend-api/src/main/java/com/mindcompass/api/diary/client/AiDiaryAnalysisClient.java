package com.mindcompass.api.diary.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
// Spring Boot에서 ai-api의 analyze-diary 내부 엔드포인트를 호출하는 클라이언트입니다.
public class AiDiaryAnalysisClient {

    private final WebClient aiWebClient;

    public AiDiaryAnalysisClient(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public AnalyzeDiaryResponse analyze(AnalyzeDiaryRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/analyze-diary")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnalyzeDiaryResponse.class)
                .block();
    }

    // ai-api analyze-diary 요청 계약입니다.
    public record AnalyzeDiaryRequest(
            Long userId,
            Long diaryId,
            String content,
            String writtenAt
    ) {
    }

    // ai-api analyze-diary 응답 계약입니다.
    public record AnalyzeDiaryResponse(
            String primaryEmotion,
            Integer emotionIntensity,
            List<String> emotionTags,
            String summary,
            BigDecimal confidence
    ) {
    }
}
