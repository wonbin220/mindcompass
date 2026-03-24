package com.mindcompass.api.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// backend-api가 어떤 내부 AI 서버를 호출할지 결정하는 설정 프로퍼티입니다.
@ConfigurationProperties(prefix = "app.ai")
public class AiEndpointProperties {

    private String provider = "spring-ai";
    private String springBaseUrl = "http://localhost:8001";
    private String fastapiBaseUrl = "http://localhost:8002";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSpringBaseUrl() {
        return springBaseUrl;
    }

    public void setSpringBaseUrl(String springBaseUrl) {
        this.springBaseUrl = springBaseUrl;
    }

    public String getFastapiBaseUrl() {
        return fastapiBaseUrl;
    }

    public void setFastapiBaseUrl(String fastapiBaseUrl) {
        this.fastapiBaseUrl = fastapiBaseUrl;
    }

    public String resolveBaseUrl() {
        String normalizedProvider = provider == null ? "spring-ai" : provider.trim().toLowerCase();
        return switch (normalizedProvider) {
            case "fastapi" -> requireBaseUrl(fastapiBaseUrl, "fastapiBaseUrl");
            case "spring-ai", "springai" -> requireBaseUrl(springBaseUrl, "springBaseUrl");
            default -> throw new IllegalStateException("지원하지 않는 app.ai.provider 입니다: " + provider);
        };
    }

    private String requireBaseUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("app.ai." + fieldName + " 값이 비어 있습니다.");
        }
        return value;
    }
}
