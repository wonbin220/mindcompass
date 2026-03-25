// 위험도 평가 프롬프트 응답을 가져오는 클라이언트 인터페이스입니다.
package com.mindcompass.aiapi.service;

@FunctionalInterface
public interface RiskScorePromptClient {

    String complete(String systemPrompt, String userPrompt);
}