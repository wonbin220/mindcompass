package com.mindcompass.api.report;

// 리포트 컨트롤러의 조회 계약과 파라미터 처리를 검증하는 WebMvc 테스트다.

import com.mindcompass.api.auth.security.JwtAuthenticationFilter;
import com.mindcompass.api.common.exception.GlobalExceptionHandler;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.report.controller.ReportController;
import com.mindcompass.api.report.dto.response.EmotionCountResponse;
import com.mindcompass.api.report.dto.response.EmotionTrendPointResponse;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.MonthlyRiskTrendResponse;
import com.mindcompass.api.report.dto.response.RiskSummaryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendPointResponse;
import com.mindcompass.api.report.dto.response.WeeklyEmotionTrendResponse;
import com.mindcompass.api.report.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getMonthlySummaryReturnsAggregatedResponse() throws Exception {
        when(reportService.getMonthlySummary(isNull(), eq(2026), eq(3)))
                .thenReturn(new MonthlyReportResponse(
                        2026,
                        3,
                        12L,
                        BigDecimal.valueOf(3.58),
                        List.of(new EmotionCountResponse(PrimaryEmotion.ANXIOUS, 4)),
                        new RiskSummaryResponse(2, 1)
                ));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .param("year", "2026")
                        .param("month", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(3))
                .andExpect(jsonPath("$.diaryCount").value(12))
                .andExpect(jsonPath("$.topPrimaryEmotions[0].emotion").value("ANXIOUS"))
                .andExpect(jsonPath("$.riskSummary.mediumCount").value(2))
                .andExpect(jsonPath("$.riskSummary.highCount").value(1));
    }

    @Test
    void getWeeklyEmotionTrendReturnsSevenDayItems() throws Exception {
        when(reportService.getWeeklyEmotionTrend(isNull(), isNull()))
                .thenReturn(new WeeklyEmotionTrendResponse(
                        LocalDate.of(2026, 3, 18),
                        LocalDate.of(2026, 3, 24),
                        List.of(
                                EmotionTrendPointResponse.empty(LocalDate.of(2026, 3, 18)),
                                new EmotionTrendPointResponse(LocalDate.of(2026, 3, 19), true, 1, PrimaryEmotion.CALM, 2)
                        )
                ));

        mockMvc.perform(get("/api/v1/reports/emotions/weekly")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-03-18"))
                .andExpect(jsonPath("$.items[0].hasDiary").value(false))
                .andExpect(jsonPath("$.items[1].primaryEmotion").value("CALM"));
    }

    @Test
    void getWeeklyEmotionTrendAcceptsAnchorDate() throws Exception {
        when(reportService.getWeeklyEmotionTrend(isNull(), eq(LocalDate.of(2026, 3, 18))))
                .thenReturn(new WeeklyEmotionTrendResponse(
                        LocalDate.of(2026, 3, 12),
                        LocalDate.of(2026, 3, 18),
                        List.of(EmotionTrendPointResponse.empty(LocalDate.of(2026, 3, 12)))
                ));

        mockMvc.perform(get("/api/v1/reports/emotions/weekly")
                        .param("date", "2026-03-18")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-03-12"))
                .andExpect(jsonPath("$.endDate").value("2026-03-18"));
    }

    @Test
    void getMonthlyRiskTrendReturnsDailyCounts() throws Exception {
        when(reportService.getMonthlyRiskTrend(isNull(), eq(2026), eq(3)))
                .thenReturn(new MonthlyRiskTrendResponse(
                        2026,
                        3,
                        List.of(
                                new RiskTrendPointResponse(LocalDate.of(2026, 3, 1), 0, 0),
                                new RiskTrendPointResponse(LocalDate.of(2026, 3, 21), 1, 1)
                        )
                ));

        mockMvc.perform(get("/api/v1/reports/risks/monthly")
                        .param("year", "2026")
                        .param("month", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items[1].mediumCount").value(1))
                .andExpect(jsonPath("$.items[1].highCount").value(1));
    }

    @Test
    void getMonthlySummaryReturnsBadRequestWhenMonthIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .param("year", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
