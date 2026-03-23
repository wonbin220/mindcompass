package com.mindcompass.api.calendar.controller;

// 캘린더 화면에 필요한 월간 감정 조회와 일별 요약 API를 받는 컨트롤러다.

import com.mindcompass.api.calendar.dto.response.DailyEmotionSummaryResponse;
import com.mindcompass.api.calendar.dto.response.MonthlyEmotionCalendarResponse;
import com.mindcompass.api.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Operation(summary = "월간 감정 캘린더 조회")
    @GetMapping("/monthly-emotions")
    public MonthlyEmotionCalendarResponse getMonthlyEmotions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 연도", example = "2026")
            @RequestParam int year,
            @Parameter(description = "조회 월", example = "3")
            @RequestParam int month
    ) {
        return calendarService.getMonthlyEmotions(userId, year, month);
    }

    @Operation(summary = "일별 감정 요약 조회")
    @GetMapping("/daily-summary")
    public DailyEmotionSummaryResponse getDailySummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 날짜", example = "2026-03-21")
            @RequestParam LocalDate date
    ) {
        return calendarService.getDailySummary(userId, date);
    }
}
