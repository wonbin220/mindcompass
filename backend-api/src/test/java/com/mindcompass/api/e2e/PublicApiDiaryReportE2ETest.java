package com.mindcompass.api.e2e;

// Diary 작성과 Report 조회 공용 흐름을 검증하는 E2E 테스트다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicApiDiaryReportE2ETest {

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
    void signupLoginDiaryAndReportFlowWorks() throws Exception {
        when(aiDiaryAnalysisClient.analyze(ArgumentMatchers.any()))
                .thenReturn(new AiDiaryAnalysisClient.AnalyzeDiaryResponse(
                        "ANXIOUS",
                        4,
                        List.of("ANXIOUS"),
                        "Diary analysis result",
                        BigDecimal.valueOf(0.81)
                ));
        when(aiSafetyClient.scoreRisk(ArgumentMatchers.argThat(
                request -> request != null && "DIARY".equals(request.sourceType())
        )))
                .thenReturn(new AiSafetyClient.RiskScoreResponse(
                        "MEDIUM",
                        BigDecimal.valueOf(0.65),
                        List.of("DISTRESS_ESCALATION"),
                        "SUPPORTIVE_RESPONSE"
                ));

        String accessToken = signupAndLogin("e2e-user@test.com", "password123!", "e2e-user");

        mockMvc.perform(post("/api/v1/diaries")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E diary",
                                  "content": "Today was heavy but I still recorded it.",
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
                                """.formatted(LocalDateTime.now().minusHours(1))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", bearer(accessToken))
                        .param("year", String.valueOf(LocalDateTime.now().getYear()))
                        .param("month", String.valueOf(LocalDateTime.now().getMonthValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.diaryCount").value(1));

        mockMvc.perform(get("/api/v1/reports/emotions/weekly")
                        .header("Authorization", bearer(accessToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(7));

        mockMvc.perform(get("/api/v1/reports/risks/monthly")
                        .header("Authorization", bearer(accessToken))
                        .param("year", String.valueOf(LocalDateTime.now().getYear()))
                        .param("month", String.valueOf(LocalDateTime.now().getMonthValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray());
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
                .andExpect(jsonPath("$.accessToken").exists())
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
