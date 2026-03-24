package com.mindcompass.api.e2e;

// Diary AI 실패 시 저장 우선 fallback 흐름을 검증하는 E2E 테스트다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicApiDiaryFallbackE2ETest {

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
    void diaryCreateSucceedsWhenAnalyzeAndRiskScoreBothFail() throws Exception {
        doThrow(new RuntimeException("analyze-diary unavailable"))
                .when(aiDiaryAnalysisClient)
                .analyze(ArgumentMatchers.any());
        doThrow(new RuntimeException("risk-score unavailable"))
                .when(aiSafetyClient)
                .scoreRisk(ArgumentMatchers.argThat(
                        request -> request != null && "DIARY".equals(request.sourceType())
                ));

        String accessToken = signupAndLogin("diary-fallback-e2e@test.com", "password123!", "diary-fallback-user");

        mockMvc.perform(post("/api/v1/diaries")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Diary fallback E2E",
                                  "content": "Today was heavy, but I still wanted to record it.",
                                  "primaryEmotion": "OVERWHELMED",
                                  "emotionIntensity": 5,
                                  "writtenAt": "%s",
                                  "emotionTags": [
                                    {
                                      "emotionCode": "OVERWHELMED",
                                      "intensity": 5
                                    }
                                  ]
                                }
                                """.formatted(LocalDateTime.now().minusMinutes(30))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.diaryId").exists())
                .andExpect(jsonPath("$.title").value("Diary fallback E2E"))
                .andExpect(jsonPath("$.primaryEmotion").value("OVERWHELMED"))
                .andExpect(jsonPath("$.riskLevel").doesNotExist())
                .andExpect(jsonPath("$.riskScore").doesNotExist())
                .andExpect(jsonPath("$.recommendedAction").doesNotExist());
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
