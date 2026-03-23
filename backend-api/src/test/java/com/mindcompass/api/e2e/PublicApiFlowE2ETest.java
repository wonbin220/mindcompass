package com.mindcompass.api.e2e;

// 회원가입부터 diary, report, chat까지 public API 흐름을 한 번에 검증하는 E2E 성격 테스트다.

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
class PublicApiFlowE2ETest {

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
    void signupLoginDiaryReportAndChatFlowWorks() throws Exception {
        when(aiDiaryAnalysisClient.analyze(ArgumentMatchers.any()))
                .thenReturn(new AiDiaryAnalysisClient.AnalyzeDiaryResponse(
                        "ANXIOUS",
                        4,
                        List.of("ANXIOUS"),
                        "불안이 크게 느껴진 하루입니다.",
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
                        "오늘 있었던 일 중 가장 마음에 남는 장면을 하나만 같이 정리해볼까요?",
                        BigDecimal.valueOf(0.77),
                        "NORMAL"
                ));

        String email = "e2e-user@test.com";
        String password = "password123!";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "e2e-user",
                                  "email": "%s",
                                  "password": "%s",
                                  "personalInfoConsentAgreed": true,
                                  "serviceTermsAgreed": true
                                }
                                """.formatted(email, password)))
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

        String accessToken = read(loginResult, "accessToken");

        MvcResult diaryResult = mockMvc.perform(post("/api/v1/diaries")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E 일기",
                                  "content": "아무도 없고 너무 힘들어서 버티기 힘들어요.",
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
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andReturn();

        long diaryId = Long.parseLong(read(diaryResult, "diaryId"));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", bearer(accessToken))
                        .param("year", String.valueOf(LocalDateTime.now().getYear()))
                        .param("month", String.valueOf(LocalDateTime.now().getMonthValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.diaryCount").value(1));

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "E2E 채팅 세션",
                                  "sourceDiaryId": %d
                                }
                                """.formatted(diaryId)))
                .andExpect(status().isCreated())
                .andReturn();

        long sessionId = Long.parseLong(read(sessionResult, "sessionId"));

        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/messages", sessionId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "오늘 너무 불안해서 아무것도 못 했어요."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.responseType").value("NORMAL"))
                .andExpect(jsonPath("$.assistantReply").value("오늘 있었던 일 중 가장 마음에 남는 장면을 하나만 같이 정리해볼까요?"));
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

        String email = "safety-e2e@test.com";
        String password = "password123!";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "safety-user",
                                  "email": "%s",
                                  "password": "%s",
                                  "personalInfoConsentAgreed": true,
                                  "serviceTermsAgreed": true
                                }
                                """.formatted(email, password)))
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

        String accessToken = read(loginResult, "accessToken");

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/chat/sessions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "고위험 E2E 세션"
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
                                  "message": "다 끝내고 싶고 사라지고 싶어요."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseType").value("SAFETY"));
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String read(MvcResult result, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get(fieldName).asText();
    }
}
