package com.mindcompass.api.security;

// 보호 API의 무인증/잘못된 토큰 응답과 요청 추적 헤더를 검증하는 통합 테스트다.

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedApiReturnsForbiddenWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void protectedApiReturnsForbiddenWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "보안 테스트 세션"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"));
    }
}
