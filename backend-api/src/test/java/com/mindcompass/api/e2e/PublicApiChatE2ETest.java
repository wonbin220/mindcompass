package com.mindcompass.api.e2e;

// Chat 기본 응답과 안전 응답 흐름을 검증하는 E2E 테스트다.

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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicApiChatE2ETest {

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
    void chatFlowReturnsNormalReplyWhenRiskIsLow() throws Exception {
        when(aiSafetyClient.scoreRisk(ArgumentMatchers.argThat(
                request -> request != null && "CHAT_MESSAGE".equals(request.sourceType())
        )))
                .thenReturn(new AiSafetyClient.RiskScoreResponse(
                        "LOW",
                        BigDecimal.valueOf(0.10),
                        List.of(),
                        "NORMAL_RESPONSE"
                ));
        when(aiChatClient.generateReply(ArgumentMatchers.any()))
                .thenReturn(new AiChatClient.GenerateReplyResponse(
                        "Let's sort through one small step together.",
                        BigDecimal.valueOf(0.77),
                        "NORMAL"
                ));

        String accessToken = signupAndLogin("chat-e2e@test.com", "password123!", "chat-user");

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E chat session"
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
                                  "message": "I feel uneasy but I want to keep going."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.responseType").value("NORMAL"))
                .andExpect(jsonPath("$.assistantReply").value("Let's sort through one small step together."));
    }

    @Test
    void highRiskChatFlowReturnsSafetyResponse() throws Exception {
        when(aiSafetyClient.scoreRisk(ArgumentMatchers.any()))
                .thenReturn(new AiSafetyClient.RiskScoreResponse(
                        "HIGH",
                        BigDecimal.valueOf(0.95),
                        List.of("SELF_HARM_IMPLICIT"),
                        "SAFETY_RESPONSE"
                ));

        String accessToken = signupAndLogin("safety-e2e@test.com", "password123!", "safety-user");

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "High risk E2E session"
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
                                  "message": "I want everything to end."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseType").value("SAFETY"));
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
