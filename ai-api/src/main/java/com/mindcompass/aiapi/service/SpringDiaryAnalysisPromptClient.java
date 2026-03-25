// 일기 분석 프롬프트를 Spring AI ChatClient로 호출하는 구현체입니다.
package com.mindcompass.aiapi.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SpringDiaryAnalysisPromptClient implements DiaryAnalysisPromptClient {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringDiaryAnalysisPromptClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return null;
        }

        return builder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}