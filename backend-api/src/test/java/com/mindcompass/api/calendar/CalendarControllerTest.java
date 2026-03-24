package com.mindcompass.api.calendar;

// Calendar 월간/일별 조회 HTTP 계약을 검증하는 WebMvc 테스트다.

import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.calendar.controller.CalendarController;
import com.mindcompass.api.calendar.dto.response.CalendarDayEmotionResponse;
import com.mindcompass.api.calendar.dto.response.DailyEmotionSummaryResponse;
import com.mindcompass.api.calendar.dto.response.MonthlyEmotionCalendarResponse;
import com.mindcompass.api.calendar.service.CalendarService;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMonthlyEmotionsReturnsCalendarResponse() throws Exception {
        when(calendarService.getMonthlyEmotions(isNull(), eq(2026), eq(3)))
                .thenReturn(new MonthlyEmotionCalendarResponse(
                        2026,
                        3,
                        List.of(
                                new CalendarDayEmotionResponse(
                                        LocalDate.of(2026, 3, 5),
                                        true,
                                        2,
                                        PrimaryEmotion.CALM,
                                        3,
                                        List.of()
                                )
                        )
                ));

        mockMvc.perform(get("/api/v1/calendar/monthly-emotions")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(3))
                .andExpect(jsonPath("$.days[0].date").value("2026-03-05"))
                .andExpect(jsonPath("$.days[0].diaryCount").value(2));
    }

    @Test
    void getDailySummaryReturnsSummaryResponse() throws Exception {
        when(calendarService.getDailySummary(isNull(), eq(LocalDate.of(2026, 3, 5))))
                .thenReturn(new DailyEmotionSummaryResponse(
                        LocalDate.of(2026, 3, 5),
                        true,
                        1,
                        PrimaryEmotion.CALM,
                        3,
                        List.of(),
                        new com.mindcompass.api.diary.dto.response.DiarySummaryResponse(
                                10L,
                                "Daily diary",
                                "preview",
                                PrimaryEmotion.CALM,
                                3,
                                LocalDateTime.of(2026, 3, 5, 22, 0)
                        )
                ));

        mockMvc.perform(get("/api/v1/calendar/daily-summary")
                        .param("date", "2026-03-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-05"))
                .andExpect(jsonPath("$.hasDiary").value(true))
                .andExpect(jsonPath("$.latestDiary.diaryId").value(10));
    }
}
