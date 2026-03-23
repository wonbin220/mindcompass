package com.mindcompass.api.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
// 내부 ai-api 호출에 사용할 WebClient를 만드는 설정 클래스입니다.
public class AiClientConfig {

    @Bean
    public WebClient aiWebClient(@Value("${app.ai.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
