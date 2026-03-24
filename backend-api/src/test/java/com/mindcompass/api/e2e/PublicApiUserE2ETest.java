package com.mindcompass.api.e2e;

// User 내 정보 조회 진입 흐름을 독립적으로 검증하는 E2E 테스트다.

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiUserE2ETest extends PublicApiE2ESupport {

    @Test
    void userMeFlowWorksAfterLogin() throws Exception {
        AuthTokens tokens = signupAndLoginTokens("user-me-e2e@test.com", "password123!", "user-me");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(tokens.accessToken()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.email").value("user-me-e2e@test.com"))
                .andExpect(jsonPath("$.nickname").value("user-me"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.settings.responseMode").exists());
    }
}
