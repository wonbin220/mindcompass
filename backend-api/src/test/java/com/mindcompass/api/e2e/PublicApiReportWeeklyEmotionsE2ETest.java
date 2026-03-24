package com.mindcompass.api.e2e;

// Report 주간 감정 추이 조회 흐름을 독립적으로 검증하는 E2E 테스트다.

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiReportWeeklyEmotionsE2ETest extends PublicApiE2ESupport {

    @Test
    void weeklyEmotionTrendFlowWorks() throws Exception {
        stubDiaryAiSuccess();
        String accessToken = signupAndLogin("report-weekly-e2e@test.com", "password123!", "report-weekly-user");
        createDiaryForReport(accessToken, "Report weekly diary");

        mockMvc.perform(get("/api/v1/reports/emotions/weekly")
                        .header("Authorization", bearer(accessToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(7));
    }
}
