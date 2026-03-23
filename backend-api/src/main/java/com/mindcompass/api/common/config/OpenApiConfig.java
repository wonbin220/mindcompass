package com.mindcompass.api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// backend-api Swagger 문서의 기본 정보와 JWT 인증 입력창을 설정하는 구성 클래스입니다.
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI mindCompassOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        // Swagger UI에서 JWT Bearer 토큰을 입력할 수 있게 하는 보안 스키마입니다.
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // 보호 API 테스트 시 전역 Authorization 헤더를 사용할 수 있게 합니다.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .info(new Info()
                        .title("Mind Compass Backend API")
                        .version("v1")
                        .description("Spring Boot 공개 API의 인증/일기 MVP 테스트용 문서입니다."));
    }
}
