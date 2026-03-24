package com.mindcompass.api.infra.config;

// 내부 AI 서버 전환 전략이 설정값대로 동작하는지 검증하는 단위 테스트다.

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiEndpointPropertiesTest {

    @Test
    void resolveBaseUrlReturnsSpringAiUrlByDefault() {
        AiEndpointProperties properties = new AiEndpointProperties();

        assertThat(properties.resolveBaseUrl()).isEqualTo("http://localhost:8001");
    }

    @Test
    void resolveBaseUrlReturnsFastApiUrlWhenProviderIsFastapi() {
        AiEndpointProperties properties = new AiEndpointProperties();
        properties.setProvider("fastapi");
        properties.setFastapiBaseUrl("http://localhost:8002");

        assertThat(properties.resolveBaseUrl()).isEqualTo("http://localhost:8002");
    }

    @Test
    void resolveBaseUrlRejectsUnsupportedProvider() {
        AiEndpointProperties properties = new AiEndpointProperties();
        properties.setProvider("unknown");

        assertThatThrownBy(properties::resolveBaseUrl)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("지원하지 않는");
    }
}
