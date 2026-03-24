// 비교용 상담 답변을 생성하고 향후 Spring AI 연결 지점을 남기는 서비스입니다.
package com.mindcompass.aiapi.service;

import com.mindcompass.aiapi.dto.GenerateReplyRequest;
import com.mindcompass.aiapi.dto.GenerateReplyResponse;
import java.math.BigDecimal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ReplyGenerationService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public ReplyGenerationService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    public GenerateReplyResponse generate(GenerateReplyRequest request) {
        String message = request.message().trim();
        if (message.isEmpty()) {
            return new GenerateReplyResponse(
                    "지금 마음을 한 문장만 더 적어주시면 더 잘 도와드릴 수 있어요.",
                    BigDecimal.valueOf(0.15),
                    "FALLBACK"
            );
        }

        String suffix = chatClientBuilderProvider.getIfAvailable() == null
                ? "현재는 Spring AI 연결 전 기본 응답으로 반환합니다."
                : "현재는 Spring AI 비교용 기본 응답으로 반환합니다.";

        return new GenerateReplyResponse(
                "'" + message + "'라고 느끼셨군요. 그 마음을 조금 더 안전하게 풀어낼 수 있도록 함께 정리해볼게요. " + suffix,
                BigDecimal.valueOf(0.65),
                "NORMAL"
        );
    }
}
