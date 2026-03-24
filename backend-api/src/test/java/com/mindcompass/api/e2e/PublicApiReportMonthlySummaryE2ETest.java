package com.mindcompass.api.e2e;

// Report 월간 요약 조회 흐름을 독립적으로 검증하는 E2E 테스트다.

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiReportMonthlySummaryE2ETest extends PublicApiE2ESupport {

    @Test
    void monthlySummaryFlowWorks() throws Exception {
        stubDiaryAiSuccess();
        String accessToken = signupAndLogin("report-monthly-e2e@test.com", "password123!", "report-monthly-user");
        createDiaryForReport(accessToken, "Report monthly diary");

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", bearer(accessToken))
                        .param("year", String.valueOf(LocalDateTime.now().getYear()))
                        .param("month", String.valueOf(LocalDateTime.now().getMonthValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.year").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.month").value(LocalDateTime.now().getMonthValue()))
                .andExpect(jsonPath("$.diaryCount").value(1));
    }
}
