package com.mindcompass.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 백엔드 전역에서 재사용할 Jackson ObjectMapper 빈을 등록하는 설정 클래스입니다.
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
