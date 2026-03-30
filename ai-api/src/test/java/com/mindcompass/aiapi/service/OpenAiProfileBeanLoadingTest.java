// OpenAI 프로필 계약에 맞는 PromptClient 선택을 최소 컨텍스트로 검증하는 테스트입니다.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiProfileBeanLoadingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class)
            .withBean(DiaryAnalysisService.class)
            .withBean(RiskScoreService.class)
            .withBean(ReplyGenerationService.class)
            .withBean(SpringDiaryAnalysisPromptClient.class)
            .withBean(SpringRiskScorePromptClient.class)
            .withBean(SpringReplyGenerationPromptClient.class)
            .withBean(NoOpDiaryAnalysisPromptClient.class)
            .withBean(NoOpRiskScorePromptClient.class)
            .withBean(NoOpReplyGenerationPromptClient.class);

    @Test
    void devProfileContractLoadsNoOpPromptClients() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "ai.openai.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(DiaryAnalysisPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(RiskScorePromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(ReplyGenerationPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(NoOpDiaryAnalysisPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(NoOpRiskScorePromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(NoOpReplyGenerationPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(SpringDiaryAnalysisPromptClient.class)).isEmpty();
                    assertThat(context.getBeansOfType(SpringRiskScorePromptClient.class)).isEmpty();
                    assertThat(context.getBeansOfType(SpringReplyGenerationPromptClient.class)).isEmpty();
                });
    }

    @Test
    void manualProfileContractLoadsSpringPromptClients() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=manual",
                        "ai.openai.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(DiaryAnalysisPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(RiskScorePromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(ReplyGenerationPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(SpringDiaryAnalysisPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(SpringRiskScorePromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(SpringReplyGenerationPromptClient.class)).hasSize(1);
                    assertThat(context.getBeansOfType(NoOpDiaryAnalysisPromptClient.class)).isEmpty();
                    assertThat(context.getBeansOfType(NoOpRiskScorePromptClient.class)).isEmpty();
                    assertThat(context.getBeansOfType(NoOpReplyGenerationPromptClient.class)).isEmpty();
                });
    }
}
