// Spring AI 비교용 ai-api 기본 컨텍스트 로딩 테스트입니다.
package com.mindcompass.aiapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "ai.openai.enabled=false"
})
class AiApiApplicationTests {

    @MockitoBean
    private JdbcClient jdbcClient;

    @Test
    void contextLoads() {
    }
}
