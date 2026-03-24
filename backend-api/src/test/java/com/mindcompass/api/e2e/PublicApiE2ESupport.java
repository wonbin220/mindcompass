package com.mindcompass.api.e2e;

// 공개 API E2E 시나리오에서 공통 로그인과 일기 생성 흐름을 재사용하는 지원 클래스다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.chat.client.AiChatClient;
import com.mindcompass.api.chat.client.AiSafetyClient;
import com.mindcompass.api.diary.client.AiDiaryAnalysisClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
abstract class PublicApiE2ESupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected AiDiaryAnalysisClient aiDiaryAnalysisClient;

    @MockitoBean
    protected AiSafetyClient aiSafetyClient;

    @MockitoBean
    protected AiChatClient aiChatClient;

    protected void stubDiaryAiSuccess() {
        when(aiDiaryAnalysisClient.analyze(any()))
                .thenReturn(new AiDiaryAnalysisClient.AnalyzeDiaryResponse(
                        "ANXIOUS",
                        4,
                        List.of("ANXIOUS"),
                        "Diary analysis result",
                        BigDecimal.valueOf(0.81)
                ));
        when(aiSafetyClient.scoreRisk(argThat(
                request -> request != null && "DIARY".equals(request.sourceType())
        )))
                .thenReturn(new AiSafetyClient.RiskScoreResponse(
                        "MEDIUM",
                        BigDecimal.valueOf(0.65),
                        List.of("DISTRESS_ESCALATION"),
                        "SUPPORTIVE_RESPONSE"
                ));
    }

    protected void signup(String email, String password, String nickname) throws Exception {
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
    }

    protected AuthTokens signupAndLoginTokens(String email, String password, String nickname) throws Exception {
        signup(email, password, nickname);
        return login(email, password);
    }

    protected String signupAndLogin(String email, String password, String nickname) throws Exception {
        return signupAndLoginTokens(email, password, nickname).accessToken();
    }

    protected AuthTokens login(String email, String password) throws Exception {
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

        return new AuthTokens(
                read(loginResult, "accessToken"),
                read(loginResult, "refreshToken")
        );
    }

    protected void createDiaryForReport(String accessToken, String title) throws Exception {
        mockMvc.perform(post("/api/v1/diaries")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
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
                                """.formatted(title, LocalDateTime.now().minusHours(1))))
                .andExpect(status().isCreated());
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected String read(MvcResult result, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get(fieldName).asText();
    }

    protected record AuthTokens(String accessToken, String refreshToken) {
    }
}
