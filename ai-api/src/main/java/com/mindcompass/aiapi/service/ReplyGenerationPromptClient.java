// 상담 답변 프롬프트 호출을 분리하는 인터페이스다.
package com.mindcompass.aiapi.service;

@FunctionalInterface
public interface ReplyGenerationPromptClient {

    String complete(String systemPrompt, String userPrompt);
}
