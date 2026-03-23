package com.mindcompass.api.diary;

// DiaryController의 CRUD/날짜 조회 HTTP 계약을 검증하는 WebMvc 테스트다.

import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import com.mindcompass.api.diary.controller.DiaryController;
import com.mindcompass.api.diary.domain.DiaryEmotionSourceType;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.response.DiaryDetailResponse;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.dto.response.EmotionTagResponse;
import com.mindcompass.api.diary.service.DiaryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiaryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryService diaryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createDiaryReturnsCreatedResponse() throws Exception {
        when(diaryService.createDiary(isNull(), any()))
                .thenReturn(new DiaryDetailResponse(
                        10L,
                        1L,
                        "위험도 테스트 일기",
                        "아무도 없고 너무 힘들어서 버티기 힘들어요.",
                        PrimaryEmotion.OVERWHELMED,
                        5,
                        List.of(
                                new EmotionTagResponse(PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER),
                                new EmotionTagResponse(PrimaryEmotion.ANXIOUS, 4, DiaryEmotionSourceType.AI_ANALYSIS)
                        ),
                        "MEDIUM",
                        BigDecimal.valueOf(0.65),
                        "[\"DISTRESS_ESCALATION\",\"ISOLATION\"]",
                        "SUPPORTIVE_RESPONSE",
                        LocalDateTime.of(2026, 3, 22, 9, 0),
                        LocalDateTime.of(2026, 3, 22, 9, 1),
                        LocalDateTime.of(2026, 3, 22, 9, 1)
                ));

        mockMvc.perform(post("/api/v1/diaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "위험도 테스트 일기",
                                  "content": "아무도 없고 너무 힘들어서 버티기 힘들어요.",
                                  "primaryEmotion": "OVERWHELMED",
                                  "emotionIntensity": 5,
                                  "writtenAt": "2026-03-22T09:00:00",
                                  "emotionTags": [
                                    {
                                      "emotionCode": "OVERWHELMED",
                                      "intensity": 5
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diaryId").value(10))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.recommendedAction").value("SUPPORTIVE_RESPONSE"));
    }

    @Test
    void getDiaryReturnsRiskFields() throws Exception {
        when(diaryService.getDiary(isNull(), eq(10L)))
                .thenReturn(new DiaryDetailResponse(
                        10L,
                        1L,
                        "고위험 테스트 일기",
                        "다 끝내고 싶고 사라지고 싶어요.",
                        PrimaryEmotion.OVERWHELMED,
                        5,
                        List.of(new EmotionTagResponse(PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER)),
                        "HIGH",
                        BigDecimal.valueOf(0.95),
                        "[\"HOPELESSNESS\",\"SELF_HARM_IMPLICIT\"]",
                        "SAFETY_RESPONSE",
                        LocalDateTime.of(2026, 3, 22, 10, 0),
                        LocalDateTime.of(2026, 3, 22, 10, 1),
                        LocalDateTime.of(2026, 3, 22, 10, 1)
                ));

        mockMvc.perform(get("/api/v1/diaries/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaryId").value(10))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.recommendedAction").value("SAFETY_RESPONSE"));
    }

    @Test
    void updateDiaryReturnsUpdatedResponse() throws Exception {
        when(diaryService.updateDiary(isNull(), eq(10L), any()))
                .thenReturn(new DiaryDetailResponse(
                        10L,
                        1L,
                        "수정된 일기",
                        "오늘은 조금 진정됐어요.",
                        PrimaryEmotion.CALM,
                        2,
                        List.of(new EmotionTagResponse(PrimaryEmotion.CALM, 2, DiaryEmotionSourceType.USER)),
                        "LOW",
                        BigDecimal.valueOf(0.15),
                        "[]",
                        "NORMAL_RESPONSE",
                        LocalDateTime.of(2026, 3, 22, 11, 0),
                        LocalDateTime.of(2026, 3, 22, 11, 1),
                        LocalDateTime.of(2026, 3, 22, 11, 5)
                ));

        mockMvc.perform(patch("/api/v1/diaries/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 일기",
                                  "content": "오늘은 조금 진정됐어요.",
                                  "primaryEmotion": "CALM",
                                  "emotionIntensity": 2,
                                  "writtenAt": "2026-03-22T11:00:00",
                                  "emotionTags": [
                                    {
                                      "emotionCode": "CALM",
                                      "intensity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 일기"))
                .andExpect(jsonPath("$.primaryEmotion").value("CALM"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void deleteDiaryReturnsNoContent() throws Exception {
        doNothing().when(diaryService).deleteDiary(isNull(), eq(10L));

        mockMvc.perform(delete("/api/v1/diaries/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getDiariesByDateReturnsListResponse() throws Exception {
        when(diaryService.getDiariesByDate(isNull(), eq(LocalDate.of(2026, 3, 22))))
                .thenReturn(new DiaryListResponse(
                        LocalDate.of(2026, 3, 22),
                        1,
                        List.of(
                                new DiarySummaryResponse(
                                        10L,
                                        "위험도 테스트 일기",
                                        "아무도 없고 너무 힘들어서...",
                                        PrimaryEmotion.OVERWHELMED,
                                        5,
                                        List.of(new EmotionTagResponse(PrimaryEmotion.OVERWHELMED, 5, DiaryEmotionSourceType.USER)),
                                        LocalDateTime.of(2026, 3, 22, 9, 0)
                                )
                        )
                ));

        mockMvc.perform(get("/api/v1/diaries")
                        .param("date", "2026-03-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-22"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.diaries[0].diaryId").value(10))
                .andExpect(jsonPath("$.diaries[0].primaryEmotion").value("OVERWHELMED"));
    }

    @Test
    void createDiaryReturnsBadRequestWhenEnumIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/diaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "테스트",
                                  "content": "enum 오류 테스트",
                                  "primaryEmotion": "ANXIETY",
                                  "emotionIntensity": 4,
                                  "writtenAt": "2026-03-22T12:00:00",
                                  "emotionTags": [
                                    {
                                      "emotionCode": "ANXIETY",
                                      "intensity": 4
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
