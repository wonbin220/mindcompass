package com.mindcompass.api.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AiEndpointProperties.class)
// 내부 ai-api 호출에 사용할 WebClient를 만드는 설정 클래스입니다.
public class AiClientConfig {

    @Bean
    public WebClient aiWebClient(AiEndpointProperties aiEndpointProperties) {
        return WebClient.builder()
                .baseUrl(aiEndpointProperties.resolveBaseUrl())
                .build();
    }
}
