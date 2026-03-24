package com.mindcompass.api.e2e;

// Report 월간 위험도 추이 조회 흐름을 독립적으로 검증하는 E2E 테스트다.

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiReportMonthlyRisksE2ETest extends PublicApiE2ESupport {

    @Test
    void monthlyRiskTrendFlowWorks() throws Exception {
        stubDiaryAiSuccess();
        String accessToken = signupAndLogin("report-risk-e2e@test.com", "password123!", "report-risk-user");
        createDiaryForReport(accessToken, "Report risk diary");

        mockMvc.perform(get("/api/v1/reports/risks/monthly")
                        .header("Authorization", bearer(accessToken))
                        .param("year", String.valueOf(LocalDateTime.now().getYear()))
                        .param("month", String.valueOf(LocalDateTime.now().getMonthValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray());
    }
}
