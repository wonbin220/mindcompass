// 개발 환경에서 OpenAI 호출을 막고 fallback만 타게 하는 no-op 구현체입니다.
package com.mindcompass.aiapi.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.openai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpDiaryAnalysisPromptClient implements DiaryAnalysisPromptClient {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return null;
    }
}
