package com.mindcompass.api.report.controller;

// 리포트 화면에서 필요한 월간 요약과 추이 조회 API를 받는 컨트롤러다.

import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.MonthlyRiskTrendResponse;
import com.mindcompass.api.report.dto.response.WeeklyEmotionTrendResponse;
import com.mindcompass.api.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "월간 리포트 요약 조회")
    @GetMapping("/monthly-summary")
    public MonthlyReportResponse getMonthlySummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 연도", example = "2026")
            @RequestParam int year,
            @Parameter(description = "조회 월", example = "3")
            @RequestParam int month
    ) {
        return reportService.getMonthlySummary(userId, year, month);
    }

    @Operation(summary = "최근 7일 감정 추이 조회")
    @GetMapping("/emotions/weekly")
    public WeeklyEmotionTrendResponse getWeeklyEmotionTrend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return reportService.getWeeklyEmotionTrend(userId);
    }

    @Operation(summary = "월간 위험도 추이 조회")
    @GetMapping("/risks/monthly")
    public MonthlyRiskTrendResponse getMonthlyRiskTrend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 연도", example = "2026")
            @RequestParam int year,
            @Parameter(description = "조회 월", example = "3")
            @RequestParam int month
    ) {
        return reportService.getMonthlyRiskTrend(userId, year, month);
    }
}
