package com.mindcompass.api.e2e;

// Chat AI 실패 시 fallback 응답 흐름을 검증하는 E2E 테스트다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicApiChatFallbackE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiDiaryAnalysisClient aiDiaryAnalysisClient;

    @MockitoBean
    private AiSafetyClient aiSafetyClient;

    @MockitoBean
    private AiChatClient aiChatClient;

    @Test
    void chatSendMessageSucceedsWithFallbackWhenAiFails() throws Exception {
        when(aiSafetyClient.scoreRisk(ArgumentMatchers.argThat(
                request -> request != null && "DIARY".equals(request.sourceType())
        )))
                .thenReturn(new AiSafetyClient.RiskScoreResponse(
                        "LOW",
                        BigDecimal.valueOf(0.10),
                        List.of(),
                        "NORMAL_RESPONSE"
                ));
        doThrow(new RuntimeException("chat ai unavailable"))
                .when(aiSafetyClient)
                .scoreRisk(ArgumentMatchers.argThat(
                        request -> request != null && "CHAT_MESSAGE".equals(request.sourceType())
                ));

        String accessToken = signupAndLogin("chat-fallback-e2e@test.com", "password123!", "chat-fallback-user");

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Chat fallback E2E"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long sessionId = Long.parseLong(read(sessionResult, "sessionId"));

        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/messages", sessionId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Today feels too heavy."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.responseType").value("FALLBACK"))
                .andExpect(jsonPath("$.assistantReply").isNotEmpty());
    }

    private String signupAndLogin(String email, String password, String nickname) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "personalInfoConsentAgreed": true,
                                  "serviceTermsAgreed": true
                                }
                                """.formatted(nickname, email, password)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return read(loginResult, "accessToken");
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String read(MvcResult result, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get(fieldName).asText();
    }
}
